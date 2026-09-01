## Exercise 6b: Carry a Reference, Not the Document

Move a few hundred KB of claim evidence out of Workflow History by passing a
reference to a document store instead of the document itself.

During this exercise, you will:

- See the same payload repeated across Activity inputs and results
- Switch the pipeline to a small summary plus an evidence reference
- Consolidate Activities that share a failure boundary
- Learn why Activity count is a poor proxy for cost

## Setup

```bash
temporal server start-dev --metrics-port 9090
```

`cd` into `6b_cost_payloads/practice/`.

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

The full instructions live in the Instruqt challenge (`track/06b-cost-payloads/assignment.md`).
Instructor notes for all three challenges are in
`exercises/6a_cost_timers/INSTRUCTOR.md`.

### This is the end of the exercise.
