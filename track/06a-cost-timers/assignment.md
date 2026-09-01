---
slug: cost-timers
type: challenge
title: 'Exercise 6a: One Wait Instead of Sixty Timers'
teaser: Measure what a single claim costs, then replace a polling loop with one wait.
notes:
- type: text
  contents: |-
    Exercise 4 was about one cost lever: moving fast in-process steps to Local
    Activities. The next three challenges are about the levers that usually cost
    more — timers, payload size, and heartbeat frequency.

    You're inheriting a **warranty damage claim** Workflow. A customer files a claim
    against a delivered order, attaching an inspection report and a bundle of photos.
    The Workflow validates the claim, looks up the warranty, analyzes the evidence,
    calculates a repair estimate, books a service slot, sends a confirmation, and
    then stays open through a 90-day appeal window in case the customer disputes it.

    It works. It's durable. It's also expensive, in ways that are invisible until
    you read the Event History.

    In this challenge you'll measure the baseline and fix the cheapest problem to
    fix. Hit **Start** when you're ready.
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
difficulty: intermediate
timelimit: 1800
enhanced_loading: null
---

## Exercise 6a: One Wait Instead of Sixty Timers

Files are in `/workspace/exercise/src/main/kotlin/fulfillment/`. Read **`Config.kt`**
first — it's the map of what this Workflow does and how much of it.

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

The Worker and the Starter each hold their own terminal, so use the
[button label="Terminal 3 - Temporal CLI" background="#444CE7"](tab-3) tab for
every command below.

**This takes about 95 seconds.** The analysis step runs for 30 seconds and the
appeal window for another 60. That's deliberate — go look at the Event History
while it's still running.

In the [button label="Temporal Web UI" background="#444CE7"](tab-4), open
`claim-CLM-2001`. Watch the `TimerStarted` / `TimerFired` pairs march down the
history one second apart. Then count what the claim cost:

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

Write these numbers down. You'll compare against them through all three challenges.

For the baseline you should see roughly:

| | baseline |
|---|---|
| history bytes | ~3.7 MB |
| total events | ~353 |
| activity tasks | 8 |
| timers | 60 |
| heartbeat RPCs | 8 |

**3.7 MB of Workflow History for one claim.** Hold that thought — it's exercise 6b.

***

### Part 2 — Fix the timers

The business rule is that a claim stays appealable for 90 days. The implementation
wakes the Workflow up over and over to check whether anything changed. In demo mode
that's 60 timers; in production, polling hourly across 90 days, it's about 2,160
timers per claim.

In `DamageClaimWorkflowImpl.kt`, find the appeal loop and replace it with:

```kotlin
Workflow.await(Duration.ofSeconds(Config.APPEAL_WINDOW_SECONDS)) { appealReceived }
```

`Workflow.await` returns as soon as the condition becomes true, and returns `false`
if the timeout expires first.

Notice that this is **more correct**, not just cheaper. The polling loop could leave
an appeal unnoticed for up to a full tick. `await` returns the moment the signal
lands. Cost and correctness point the same way here, which is not always true and is
worth noticing when it is.

***

### Part 3 — Prove it

Restart the Worker and run a fresh claim. While it's still inside the appeal window,
send the appeal signal from
[button label="Terminal 3 - Temporal CLI" background="#444CE7"](tab-3):

```bash,run
temporal workflow signal --workflow-id claim-CLM-2001 --name submitAppeal
```

The Workflow should return `status=APPEALED` immediately rather than waiting out the
window. Then re-count: **timers should be 1.**

Look at the event count too. It drops from **353 to 58** — far more than the 59
timers you removed, because every tick also woke the Workflow, and each of those
wake-ups cost a WorkflowTask triplet of its own. One loop was generating roughly
five events per iteration.

What barely moves is the size: ~3.7 MB becomes ~3.6 MB, because the evidence is
untouched. Activity count is unchanged at 8. Those are the next two challenges.

***

Click **Check** when done, or **Solve** to see the reference solution.
