## Exercise 6: Payloads, Timers, and Heartbeats

During this exercise, you will:

- Measure what a single Workflow execution costs by reading its Event History
- Replace a polling timer loop with a single `Workflow.await()` over the whole window
- Move a large business document out of Workflow History and carry a reference instead
- Set heartbeat frequency from failure-detection needs rather than loop iterations

Exercise 4 covered one cost lever: Local Activities for fast in-process steps.
This exercise covers the three that usually matter more — payload size, timer
count, and heartbeat frequency — using a warranty damage claim Workflow that is
correct and durable but considerably more expensive than it needs to be.

## Setup

```bash
temporal server start-dev
```

`cd` into `6_cost_deep_dive/practice/`.

## Part 1: Measure the baseline

```bash
# Terminal 1
gradle runWorker

# Terminal 2
gradle runStarter
```

One claim takes about 95 seconds — 30 for the evidence analysis, 60 for the
appeal window. Open `claim-CLM-2001` in the Web UI while it runs.

Then count what it cost:

```bash
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

```bash
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

Expand a few `ActivityTaskScheduled` events in the UI and look at the inputs.
The same evidence blob appears in nearly all of them.

On Temporal Cloud the equivalent view is `temporal_cloud_v1_billable_action_count`
broken down by `temporal_workflow_type` and `action_type`, where the concentration
sits in `schedule_activity`, `start_timer`, and `record_activity_heartbeat`. The
dev server has no billing meter, so Event History is the meter here.

## Part A: One wait instead of a timer loop

In `DamageClaimWorkflowImpl.kt`, replace the appeal `for` loop with:

```kotlin
Workflow.await(Duration.ofSeconds(Config.APPEAL_WINDOW_SECONDS)) { appealReceived }
```

Verify the signal still works while a claim is in its appeal window:

```bash
temporal workflow signal --workflow-id claim-CLM-2001 --name submitAppeal
```

## Part B: Carry a reference, not the document

`LeanClaimActivities` is already written and wired up as `leanActivities` and
`leanAnalysis`. Every method takes a small `ClaimSummary` holding an `evidenceRef`;
Activities that need the evidence read it from `EvidenceStore`.

Swap the pipeline over to it, then return `evidenceRef = summary.evidenceRef`
instead of `claim = workingClaim`.

Note that `prepareClaim` consolidates `validateClaim`, `normalizeCustomerName`,
and `extractProductMetadata`. Those were pure in-process transformations sharing
one failure boundary — they never needed separate Activities. Unlike Exercise 4,
the fix here isn't "make them local," it's "they're one operation."

## Part C: Heartbeat on progress, not on the clock

In `EvidenceAnalyzer.kt`:

```kotlin
if (step % Config.HEARTBEAT_EVERY_N_STEPS == 0) {
    context.heartbeat(step)
}
```

In `Config.kt`, raise `HEARTBEAT_EVERY_N_STEPS` to 10 and
`HEARTBEAT_TIMEOUT_SECONDS` to 60.

These two are coupled: the timeout is how long the server waits before assuming
the Worker died, so it must outlast the gap between heartbeats. Raise the interval
without raising the timeout and the Activity fails mid-analysis.

The analysis is legitimately long-running and should heartbeat. The question is
how quickly you need to detect a dead Worker — not how often your loop iterates.

Note what the measurements show: the baseline calls `heartbeat()` 30 times but
produces only ~16 RPCs, because the SDK already throttles heartbeats relative to
the heartbeat timeout. Raising the timeout to 60s takes it to ~2. The timeout is
the real cost lever.

## Part 3: Measure again

| | baseline | optimized | change |
|---|---|---|---|
| history bytes | ~3.7 MB | ~586 KB | −84% |
| total events | 353 | 52 | −85% |
| activity tasks | 8 | 7 | −1 |
| timers | 60 | 1 | −98% |
| heartbeat RPCs | ~16 | ~2 | −87% |

Same business outcome both times. Note that Activity count barely moved — 8 to 7,
because consolidating three Activities also added `storeEvidence`. The saving came
from what the Activities carried and from not hand-rolling a clock, not from having
fewer of them. Activity count is a poor proxy for cost.

**Discussion:** the third storage dimension this exercise can't demo locally is
retention. Large History × many executions × how long the Workflow stays open ×
the namespace's closed-Workflow retention is the storage bill. Keeping a Workflow
open so "the claim record stays available" is an application requirement wearing
a Temporal costume — set Temporal retention from operational and debugging needs.

### This is the end of the exercise.
