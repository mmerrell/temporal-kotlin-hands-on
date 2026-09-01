## Exercise 6c: Heartbeat on Progress, Not the Clock

Tune a long-running Activity's heartbeat to what failure detection actually
requires, and find out which setting really controls the cost.

During this exercise, you will:

- Count heartbeat RPCs, which never appear in Event History
- Heartbeat at checkpoints instead of every loop iteration
- See that the heartbeat timeout, not your loop, is the real cost lever
- Understand why the interval and the timeout are coupled

## Setup

```bash
temporal server start-dev --metrics-port 9090
```

`cd` into `6c_cost_heartbeats/practice/`.

```bash
# Terminal 1
gradle runWorker

# Terminal 2
gradle runStarter
```

One claim takes about 95 seconds — 30 for the evidence analysis, 60 for the
appeal window. Open `claim-CLM-2001` in the Web UI while it runs.

## Measuring

```bash
temporal workflow show --workflow-id claim-CLM-2001 --output json > /tmp/history.json
echo "history bytes:  $(wc -c < /tmp/history.json)"
echo "total events:   $(grep -c '"eventId"' /tmp/history.json)"
echo "activity tasks: $(grep -c ACTIVITY_TASK_SCHEDULED /tmp/history.json)"
echo "timers:         $(grep -c TIMER_STARTED /tmp/history.json)"
```

Heartbeats aren't stored in Event History — a Heartbeat overwrites a single "last
known progress" slot on the Activity's record so a retry can resume, rather than
appending an event. Count the server calls instead:

```bash
curl -s localhost:9090/metrics | grep 'service_requests.*RecordActivityTaskHeartbeat.*frontend'
```

## The exercise

The full instructions live in the Instruqt challenge (`track/06c-cost-heartbeats/assignment.md`).
Instructor notes for all three challenges are in
`exercises/6a_cost_timers/INSTRUCTOR.md`.

### This is the end of the exercise.
