---
slug: cost-payloads
id: uvudqoroy0gh
type: challenge
title: 'Exercise 6b: Carry a Reference, Not the Document'
teaser: Move a few hundred KB of evidence out of Workflow History without changing
  the business outcome.
notes:
- type: text
  contents: |-
    You've fixed the timers. One claim is down from 353 events to 58 — but it still
    weighs about 3.6 MB.

    The claim carries an inspection report and a photo bundle — a couple of hundred
    KB of evidence — and every Activity is handed the whole thing. That data lands
    in Workflow History on every input and every result, eight times over.

    Nobody wrote it that way to be wasteful. Passing the whole object is the obvious
    thing to do when each step might need any part of it. It's only expensive once
    you know that Temporal persists every one of those inputs and results, durably,
    for as long as the namespace retains the execution.

    Hit **Start** when you're ready.
tabs:
- id: 0uhvvbyhemds
  title: Code Editor
  type: code
  hostname: workshop
  path: /workspace/exercise
- id: trzrvkb05mrn
  title: Terminal 1 - Worker
  type: terminal
  hostname: workshop
  workdir: /workspace/exercise
- id: 0k0lhtb6jg3t
  title: Terminal 2 - Starter
  type: terminal
  hostname: workshop
  workdir: /workspace/exercise
- id: rjo4z8tzh8qh
  title: Terminal 3 - Temporal CLI
  type: terminal
  hostname: workshop
  workdir: /workspace/exercise
- id: xvo0uabkgmam
  title: Temporal Web UI
  type: service
  hostname: workshop
  path: /
  port: 8080
difficulty: advanced
timelimit: 1800
enhanced_loading: null
---

## Exercise 6b: Carry a Reference, Not the Document

Your work is in **`DamageClaimWorkflowImpl.kt`**. Two new files came with this
challenge — read them first:

- **`LeanClaimActivities.kt`** — the same pipeline, but every method takes a small
  `ClaimSummary` carrying an `evidenceRef` instead of the full claim
- **`EvidenceStore.kt`** — stands in for the document store the claims system
  already has

***

### Part 1 — See the problem

Run a claim, open `claim-CLM-2001` in the
[button label="Temporal Web UI" background="#444CE7"](tab-4), expand a few
`ActivityTaskScheduled` events, and look at the **input** field. The same evidence
blob is in nearly all of them. Then look at the matching results.

The Web UI will feel sluggish loading this history. That's the lesson made tactile.

***

### Part 2 — Switch to the lean pipeline

`leanActivities` and `leanAnalysis` are already wired up in the Workflow. Nothing
calls them yet. Swap the pipeline over:

```kotlin
var summary = leanActivities.storeEvidence(claim)
summary = leanActivities.prepareClaim(summary)

val warrantyStatus = leanActivities.lookupWarrantyStatus(summary)
val findings = leanAnalysis.analyzeDamageEvidence(summary)
val estimate = leanActivities.calculateRepairEstimate(summary, warrantyStatus)
val serviceSlot = leanActivities.reserveServiceSlot(summary)
val confirmationId = leanActivities.sendClaimConfirmation(summary, estimate, serviceSlot)
```

Then in the returned `ClaimResult`, replace `claim = workingClaim` with
`evidenceRef = summary.evidenceRef`.

Two things worth noticing as you do it:

**`prepareClaim` is three of the old Activities in one.** `validateClaim`,
`normalizeCustomerName`, and `extractProductMetadata` were pure in-process
transformations that failed and retried together, so they never needed separate
Activity boundaries. This is the same judgment call as Exercise 4 — but the answer
here isn't "make them Local Activities," it's "they're one operation." Choose
Activity boundaries from failure semantics, not from how your functions happen to
be named.

**The evidence still hits History once**, in the Workflow input and in
`storeEvidence`. Once is the cost of accepting the claim. Eight times was the bug.
If you wanted to remove even that, the Starter would upload the evidence and pass a
reference in, so the Workflow never sees the document at all.

> `EvidenceStore` is an in-process map so this exercise needs no infrastructure.
> It isn't durable and isn't shared between Workers. Don't copy that part into
> anything real — the point is only that the document lives *outside* Temporal.

***

### Part 3 — Measure again

Restart the Worker, run a fresh claim, and re-count:

```bash,run
temporal workflow show --workflow-id claim-CLM-2001 --output json > /tmp/history.json
echo "history bytes:  $(wc -c < /tmp/history.json)"
echo "total events:   $(grep -c '"eventId"' /tmp/history.json)"
echo "activity tasks: $(grep -c ACTIVITY_TASK_SCHEDULED /tmp/history.json)"
echo "timers:         $(grep -c TIMER_STARTED /tmp/history.json)"
```

Heartbeats aren't actually stored in Event History for a Workflow. A Heartbeat
overwrites a single "last known progress" slot on the Activity's record, so a retry
can pick up where the previous attempt died. There's no append-only trail of them to
read, because the heartbeat history isn't important to rebuilding and maintaining
state: only the "latest" matters.

Each Heartbeat is still a call to the server, though, and that call is what Temporal
Cloud bills as `record_activity_heartbeat`. The dev server counts them on its
metrics endpoint:

```bash,run
curl -s localhost:9090/metrics | grep 'service_requests.*RecordActivityTaskHeartbeat.*frontend'
```

That counter is cumulative for the session, so read it before and after a run and
take the difference.

You should see roughly:

| | after 6a | after 6b |
|---|---|---|
| history bytes | ~3.6 MB | ~586 KB |
| total events | 58 | 52 |
| activity tasks | 8 | 7 |
| timers | 1 | 1 |

Same status, same estimate, same service slot.

Notice which row barely moved. **Activity count went from 8 to 7**, because
consolidating three Activities also added `storeEvidence`. Almost none of the saving
came from having fewer Activities — it came from what they carried. Activity count
is a poor proxy for cost, and "fewer Activities" is not the lesson.

***

### One more thing: storage duration

Payload size is only one dimension. The other is how long it stays.

A large History, multiplied by many executions, multiplied by how long the Workflow
stays open, multiplied by the namespace's closed-Workflow retention — that's the
storage bill. The baseline made all four worse at once, and its justification for
staying open through the appeal window was "we need the claim record available."

That's an application requirement wearing a Temporal costume. Business and audit
artifacts belong in the system of record. Set Temporal's retention from what you
need for **operations and debugging**, not from how long the business must keep the
documents.

***

Click **Check** when done, or **Solve** to see the reference solution.
