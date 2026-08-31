## Exercise 1: Converting a Workflow

During this exercise, you will:

- Identify the problems with manual retry loops and local variable state
- Extract activity logic from an ad-hoc pipeline into Temporal Activity implementations
- Write a Temporal Workflow that orchestrates three sequential activities
- Observe durable execution in the Web UI

The starting point is `FulfillmentPipeline.kt` — a deliberately fragile implementation
using `Thread.sleep()` retries and local state. Your job is to replace it with a proper
Temporal workflow.

## Setup

Start a local Temporal server if you haven't already:
```bash
temporal server start-dev
```

Open two terminal windows and `cd` into `1_converting/practice/`.

## Part A: Implement the Activities

Open `FulfillmentActivitiesImpl.kt`. For each of the three methods:

1. Move the corresponding logic from `FulfillmentPipeline.kt`
2. Replace raw `Exception` throws with `ApplicationFailure.newFailure(message, type)`
3. Keep the `Math.random()` failure simulation — we want retries to happen

## Part B: Implement the Workflow

Open `FulfillmentWorkflowImpl.kt`:

1. Create `ActivityOptions` with a `StartToCloseTimeout` of 30 seconds
2. Create a `FulfillmentActivities` stub via `Workflow.newActivityStub(...)`
3. In `processOrder()`, call the three activities in sequence and return an `OrderResult`

## Part C: Run It

```bash
# Terminal 1
gradle runWorker

# Terminal 2
gradle runStarter
```

Open the Web UI at http://localhost:8233. Kill the worker mid-execution and restart it.
What happens?

### This is the end of the exercise.
