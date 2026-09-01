---
slug: cost-heartbeats
type: challenge
title: 'Exercise 6c: Heartbeat on Progress, Not the Clock'
teaser: Tune a long-running Activity's heartbeat to what failure detection actually
  needs — and find out which setting really controls the cost.
notes:
- type: text
  contents: |-
    Two down. The timers are one wait, the evidence travels by reference, and one
    claim now costs about 586 KB of History instead of 3.7 MB.

    One thing is left. The evidence analysis runs for 30 seconds and reports
    progress back to the server every single second, for a job that has no need for
    one-second checkpoints.

    This one is subtler than the other two, and it has a trap in it. Heartbeating is
    not the problem — a long-running Activity *should* heartbeat, because that's how
    a dead Worker gets noticed while the Activity is still in flight. The question
    is how often, and the answer isn't set where you'd expect.

    Hit **Start** when you're ready.
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
- title: Terminal 3 - Temporal CLI
  type: terminal
  hostname: workshop
  workdir: /workspace/exercise
- title: Temporal Web UI
  type: service
  hostname: workshop
  path: /
  port: 8080
difficulty: advanced
timelimit: 1800
enhanced_loading: null
---

## Exercise 6c: Heartbeat on Progress, Not the Clock

Your work is in **`EvidenceAnalyzer.kt`** and **`Config.kt`**.

***

### Part 1 — Count the heartbeats

Run a claim and read the heartbeat counter before and after:

```bash,run
curl -s localhost:9090/metrics | grep 'service_requests.*RecordActivityTaskHeartbeat.*frontend'
```

`temporal workflow show` is no help here. heartbeats aren't actually stored in Event History for a Workflow. A Heartbeat
overwrites a single "last known progress" slot on the Activity's record, so a retry
can pick up where the previous attempt died. There's no append-only trail of them to
read, because the heartbeat history isn't important to rebuilding and maintaining
state: only the "latest" matters.

Each Heartbeat is still a call to the server, and that call is what Temporal Cloud
bills as `record_activity_heartbeat`.

You can watch the slot being overwritten. While the analysis is running, from
[button label="Terminal 3 - Temporal CLI" background="#444CE7"](tab-3):

```bash,run
temporal workflow describe --workflow-id claim-CLM-2001
```

Look at `pendingActivities` — `heartbeatDetails` holds the step number from the most
recent Heartbeat, and `lastHeartbeatTime` moves each time one lands. Run it twice a
few seconds apart and watch the number change. The moment the Activity completes the
whole entry disappears: it was never part of the history.

Here's the first surprise. The analysis loop calls `heartbeat()` **30 times**, once
per region. The RPC count you just measured is **8**. The SDK is already throttling
heartbeats for you, relative to the heartbeat *timeout*: it sends at most about one
per 80% of that timeout, which at a 5-second timeout is one every four seconds. Over
a 30-second activity that is 8. Three quarters of your calls never left the Worker.

***

### Part 2 — Heartbeat at checkpoints

In `EvidenceAnalyzer.kt`, guard the heartbeat call:

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

**These two settings are coupled, and that's the real lesson.**
`HEARTBEAT_TIMEOUT_SECONDS` is how long the server waits to hear from the Activity
before assuming the Worker died. Raise the checkpoint interval without raising the
timeout and the Activity fails mid-analysis with a heartbeat timeout.

Try it if you want to see it fail: set the interval to 10 and leave the timeout at 5.

***

### Part 3 — Which setting actually mattered?

Re-run and count the heartbeat RPCs again. You should land on **1**.

Now the second surprise: **the timeout did all of that work, and the guard did none
of it.** Raising the timeout to 60s on its own — leaving the heartbeat firing every
single step — also produces exactly 1 RPC, because the SDK then throttles to one send
per 48 seconds and the activity only runs for 30.

So why write the guard at all? Two reasons, and neither is the RPC count:

- Without it you are relying on an SDK throttle you never chose. Your code says
  "checkpoint 30 times"; something else quietly decides it means once. The guard
  makes the interval you actually want explicit and reviewable.
- The guard is what makes a *longer* timeout safe to reason about. Checkpoint every
  10 steps and the meaning of the 60s timeout is clear: three progress reports, 10
  seconds apart, well inside it.

And note the trap runs the other way too. The guard **without** the timeout raise
doesn't merely fail to help — it breaks the Activity. Heartbeats 10 seconds apart
against a 5-second timeout means the server declares the Worker dead mid-analysis.

So the lever is the heartbeat timeout, and the way to choose it is:

> How quickly do I need to notice that a Worker died mid-Activity?

Then set the timeout from that answer, and heartbeat somewhat more often than it.
Don't derive either number from how often your loop happens to iterate — and don't
respond to a heartbeat cost problem by removing heartbeats from an Activity that
legitimately needs them.

***

### Part 4 — The whole arc

Re-count everything one last time:

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

Across all three challenges:

| | baseline | after 6a | after 6b | after 6c |
|---|---|---|---|---|
| history bytes | ~3.7 MB | ~3.6 MB | ~586 KB | ~586 KB |
| total events | 353 | 58 | 52 | 52 |
| activity tasks | 8 | 8 | 7 | 7 |
| timers | 60 | 1 | 1 | 1 |
| heartbeat RPCs | 8 | 8 | 8 | 1 |

The business outcome never changed: same status, same estimate, same service slot,
same appeal behavior. A claim that cost 353 events and 3.7 MB now costs 52 events
and 586 KB, and it is exactly as durable as it was before.

None of the three fixes weakened a guarantee. That's the point worth taking home —
the expensive version wasn't buying you safety with all that cost.

***

Click **Check** when done, or **Solve** to see the reference solution.
