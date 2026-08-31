## Exercise 4: Cost Optimization with Local Activities

During this exercise, you will:

- Identify which workflow steps are fast, in-process operations (no network calls)
- Convert those steps to Local Activities using `Workflow.newLocalActivityStub()`
- Observe the difference in Event History entries vs. regular activities

In Temporal Cloud, cost is driven by Actions. Each regular activity generates a
schedule/start/complete triplet. Local Activities run inside the Worker process
and produce a single `MarkerRecorded` event instead — significantly cheaper for
fast, non-network steps.

## Setup

```bash
temporal server start-dev
```

`cd` into `4_cost_optimization/practice/`.

## Part A: Create the Local Activity stub

Open `FulfillmentWorkflowImpl.kt`:

1. Create `LocalActivityOptions` with a `StartToCloseTimeout` of 5 seconds
2. Create a `LocalFulfillmentActivities` stub via `Workflow.newLocalActivityStub(...)`

Note: `LocalActivityOptions` not `ActivityOptions`, `newLocalActivityStub` not `newActivityStub`.

## Part B: Call local activities before remote ones

Add these two calls at the top of `processOrder()`, before the child workflow:

```kotlin
localActivities.validateOrder(order)
localActivities.fraudCheck(order)
```

## Part C: Run It and Compare

```bash
# Terminal 1
gradle runWorker

# Terminal 2
gradle runStarter
```

In the Web UI, open `fulfillment-ORD-1004` and inspect the Event History.
Look for `MarkerRecorded` events for the local activities vs. the
`ActivityTaskScheduled` / `ActivityTaskStarted` / `ActivityTaskCompleted`
triplet for regular activities.

**Discussion:** What steps in your own workflows could be local activities?
Think about: input validation, in-memory lookups, format conversion, fraud rules,
config from a local cache.

### This is the end of the exercise.
