---
slug: cost-deep-dive
type: challenge
title: 'Exercise 6: Payloads, Timers, and Heartbeats'
teaser: Find out why a correct, durable Workflow costs far more than it should — then
  fix it without weakening the guarantees that matter.
notes:
- type: text
  contents: |-
    Exercise 4 was about one lever: moving fast in-process steps to Local Activities.
    This one is about the three levers that usually cost more.

    You're inheriting a **warranty damage claim** Workflow. A customer files a claim
    against a delivered order, attaching an inspection report and a bundle of photos.
    The Workflow validates the claim, looks up the warranty, analyzes the evidence,
    calculates a repair estimate, books a service slot, sends a confirmation, and
    then stays open through a 90-day appeal window in case the customer disputes it.

    It works. It's durable. It's also expensive, in three ways that are invisible
    until you look at the Event History:

    - **Payload size.** The claim carries a few hundred KB of evidence, and every
      Activity is handed the whole thing. That data lands in Workflow History on
      every input and every result.
    - **Timers.** The appeal window is implemented by waking up over and over to
      check whether anything changed.
    - **Heartbeats.** The analysis step reports progress once per second, whether
      or not anything meaningful has happened.

    Nobody wrote this to be wasteful. Each choice is locally reasonable — that's
    what makes it worth studying.

    Your job is to measure it first, then fix it. Hit **Start** when you're ready.
tabs:
- title: Code Editor
  type: code
  hostname: workshop
  path: /workspace/exercise
- title: Terminal 1 - Worker
  type: terminal
  hostname: workshop
  workdir: /workspace/exercise
- title: Terminal 2 - Starter
  type: terminal
  hostname: workshop
  workdir: /workspace/exercise
- title: Temporal Web UI
  type: service
  hostname: workshop
  path: /
  port: 8080
difficulty: advanced
timelimit: 3600
enhanced_loading: null
---

## Exercise 6: Payloads, Timers, and Heartbeats

Files are in `/workspace/exercise/src/main/kotlin/fulfillment/`.

You'll edit three of them: **`DamageClaimWorkflowImpl.kt`**, **`EvidenceAnalyzer.kt`**,
and **`Config.kt`**. Everything else is context — read `Config.kt` first, it's the
map of what this Workflow does and how much of it.

***

### Part 1 — Measure the baseline

Don't change anything yet. Run it and look at what it produces.

1. Click the [button label="Terminal 1 - Worker" background="#444CE7"](tab-1) tab and start the Worker:

   ```bash,run
   gradle runWorker
   ```

2. Click the [button label="Terminal 2 - Starter" background="#444CE7"](tab-2) tab and submit one claim:

   ```bash,run
   gradle runStarter
   ```

**This takes about 95 seconds.** The analysis step runs for 30 seconds and the
appeal window for another 60. That's deliberate — go look at the Event History
while it's still running.

In the [button label="Temporal Web UI" background="#444CE7"](tab-3), open `claim-CLM-2001`.

Then, in the Starter terminal, count what one claim actually cost:

```bash,run
temporal workflow show --workflow-id claim-CLM-2001 --output json > /tmp/history.json
echo "history bytes:  $(wc -c < /tmp/history.json)"
echo "total events:   $(grep -c '"eventId"' /tmp/history.json)"
echo "activity tasks: $(grep -c ACTIVITY_TASK_SCHEDULED /tmp/history.json)"
echo "timers:         $(grep -c TIMER_STARTED /tmp/history.json)"
```

Heartbeats need a different tool, and that itself is worth knowing: **heartbeats
are not Event History events.** They update the Activity's mutable state, so
`temporal workflow show` will never show them. What they do produce is a server
RPC — the thing Temporal Cloud bills as `record_activity_heartbeat`. The dev
server counts those on its own metrics endpoint:

```bash,run
curl -s localhost:9090/metrics | grep 'service_requests.*RecordActivityTaskHeartbeat.*frontend'
```

That counter is cumulative for the whole session, so read it before and after a
run and take the difference.

For a baseline claim you should see roughly:

| | baseline |
|---|---|
| history bytes | ~3.7 MB |
| total events | ~353 |
| activity tasks | 8 |
| timers | 60 |
| heartbeat RPCs | ~16 |

**3.7 MB of Workflow History for one claim carrying 200 KB of evidence.**

Write those numbers down. You'll compare against them at the end.

Two things to notice in the UI while you're there:

- Expand a few `ActivityTaskScheduled` events and look at the **input**. The same
  evidence blob is in nearly all of them. Then look at the matching results.
- The `TimerStarted` / `TimerFired` pairs march down the history one second apart.
  In production that's hourly for 90 days — roughly 2,160 timers per claim.

> **On Temporal Cloud** you'd see this as `temporal_cloud_v1_billable_action_count`,
> broken down by `temporal_workflow_type` and `action_type`. The concentration
> would sit in `schedule_activity`, `start_timer`, and `record_activity_heartbeat`.
> The dev server has no billing meter, so the Event History is your meter here —
> the *shape* is what matters, not the invoice.

***

### Part 2 — Before you touch the code

Have a look at all three problems and decide what you'd change. Specifically:

1. Which of those Activities genuinely need their own failure and retry boundary?
2. Does Temporal need to hold the evidence for the claim to be orchestrated correctly?
3. What is the appeal loop actually waiting for, and what's the cheapest way to wait for it?
4. The Workflow stays open for the whole appeal window so "the claim record stays
   available." Is that a Temporal problem or an application problem?

Then work through the parts below.

***

### Part A — One wait instead of a timer loop

In `DamageClaimWorkflowImpl.kt`, find the appeal loop. Replace it with:

```kotlin
Workflow.await(Duration.ofSeconds(Config.APPEAL_WINDOW_SECONDS)) { appealReceived }
```

`Workflow.await` returns as soon as the condition becomes true, and returns `false`
if the timeout expires first. One timer covers the whole window — and the behavior
is *better*, not just cheaper: the old loop could leave an appeal unnoticed for up
to a full tick.

Test that the signal still works. Start a claim, and while it's in the appeal
window, signal it from the other terminal:

```bash,run
temporal workflow signal --workflow-id claim-CLM-2001 --name submitAppeal
```

The Workflow should return `status=APPEALED` immediately rather than waiting out
the window.

***

### Part B — Carry a reference, not the document

The claim's evidence belongs in the claims system's document store. Temporal
needs the claim *id* and enough state to orchestrate — not 200 KB of photos.

`LeanClaimActivities` is already written and already wired up as `leanActivities`
and `leanAnalysis`. Every method takes a small `ClaimSummary` carrying an
`evidenceRef`; the Activities that need the evidence read it from `EvidenceStore`.

Swap the pipeline over:

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

Two things worth noticing while you do this:

- **`prepareClaim` is three of the old Activities in one.** `validateClaim`,
  `normalizeCustomerName`, and `extractProductMetadata` were pure in-process
  transformations that failed and retried together, so they never needed separate
  Activity boundaries. This is the same judgment call as Exercise 4 — but here the
  answer isn't "make them Local Activities," it's "they're one operation." Choose
  Activity boundaries from failure semantics, not from how your functions happen
  to be named.
- **The evidence still hits History once**, in the Workflow input and in
  `storeEvidence`. Once is the cost of accepting the claim. Nine times was the bug.
  (If you wanted to eliminate even that, the *Starter* would upload the evidence
  and pass a reference in — worth discussing.)

***

### Part C — Heartbeat on progress, not on the clock

The analysis really does run long, and it really should heartbeat — that's how a
dead Worker gets noticed while an Activity is still in flight. The problem is the
*frequency*: once per second, for a job with no need for one-second checkpoints.

In `EvidenceAnalyzer.kt`, guard the heartbeat:

```kotlin
if (step % Config.HEARTBEAT_EVERY_N_STEPS == 0) {
    context.heartbeat(step)
}
```

Then in `Config.kt`, raise both of these:

```kotlin
const val HEARTBEAT_EVERY_N_STEPS = 10
const val HEARTBEAT_TIMEOUT_SECONDS = 60L
```

**These two settings are coupled, and that's the real lesson.** `HEARTBEAT_TIMEOUT_SECONDS`
is how long the server waits to hear from the Activity before assuming the Worker
died. If you raise the checkpoint interval without raising the timeout, the Activity
fails mid-analysis with a heartbeat timeout. Try it if you want to see it — set the
interval to 10 and leave the timeout at 5.

**Heartbeat frequency is a failure-detection decision.** Ask how quickly you need
to notice a dead Worker, then heartbeat somewhat more often than that. Don't derive
it from how often your loop happens to iterate.

There's a subtlety worth seeing in the numbers. The baseline calls `heartbeat()`
30 times, but the measured RPC count is ~16 — the SDK already throttles heartbeats
relative to the heartbeat *timeout*, so it was suppressing about half of them.
Raise the timeout to 60s and the count drops to ~2. So the timeout is the real
cost lever; the checkpoint guard mostly keeps your own code honest about what a
checkpoint means.

***

### Part 3 — Measure again

Restart the Worker, run a fresh claim, and re-run the counting commands from Part 1.
Here is what the reference solution measures:

| | baseline | optimized | change |
|---|---|---|---|
| history bytes | ~3.7 MB | ~586 KB | −84% |
| total events | 353 | 52 | −85% |
| activity tasks | 8 | 7 | −1 |
| timers | 60 | 1 | −98% |
| heartbeat RPCs | ~16 | ~2 | −87% |

Same business outcome both times — same status, same estimate, same service slot,
same appeal behavior. Only the cost changed.

Notice which row barely moved. **Activity count went from 8 to 7**, because
consolidating three Activities into one also added `storeEvidence`. Almost none of
the saving came from having fewer Activities — it came from what they carried and
from not implementing a clock by hand. That's the point of the Exercise 4 contrast:
"fewer Activities" is not the lesson, and an Activity count is a poor proxy for cost.

***

### One more thing: storage duration

Two of these problems were about how much data goes *in*. There's a third dimension
this exercise can only talk about: how long it stays.

A large History, multiplied by many executions, multiplied by how long the Workflow
stays open, multiplied by the namespace's closed-Workflow retention period — that's
the storage bill. The baseline made all four factors worse at once, and the
justification for the third one was "we need the claim record available."

That's an application requirement wearing a Temporal costume. Business and audit
artifacts belong in the system of record. Set Temporal's retention from what you
need for **operations and debugging**, not from how long the business must keep
the documents.

***

Click **Check** when done, or **Solve** to see the reference solution.
