## Exercise 1: Converting a Workflow

During this exercise, you will:

- Identify the problems with manual retry loops and local variable state
- Tell a transient failure apart from a business failure, and classify each correctly
- Express a retry policy as configuration instead of a loop
- See why a retried Activity doesn't bloat Workflow History

The starting point is `FulfillmentPipeline.kt` — a deliberately fragile implementation
using `Thread.sleep()` retries and local state. Your job is to replace it with a proper
Temporal workflow.

## Setup

Start a local Temporal server if you haven't already:
```bash
temporal server start-dev
```

Open two terminal windows and `cd` into `1_converting/practice/`.

## Part A: Two Activities, two kinds of failure

`dispatchToFulfillment` is already written — read it first, it shows the shape.
Then implement the other two in `FulfillmentActivitiesImpl.kt`. They fail for
different reasons, and the difference is the point:

- **`reserveInventory`** — the warehouse service times out. Transient, so the
  failure is **retryable**: `ApplicationFailure.newFailure(...)`. Fail attempts 1-2
  and succeed on the third, using `Activity.getExecutionContext().info.attempt`,
  so the retry is visible.
- **`processPayment`** — the card is declined above `CREDIT_LIMIT`. Retrying a
  decline just declines again, so the failure is **non-retryable**:
  `ApplicationFailure.newNonRetryableFailure(...)`.

## Part B: The Workflow

In `FulfillmentWorkflowImpl.kt`, create the stub with a 30-second
`StartToCloseTimeout` and `RetryOptions` of `maximumAttempts(5)` /
`initialInterval(2s)` — the same policy as the pipeline's `MAX_RETRIES` and
`RETRY_DELAY_MS`, except Temporal does the waiting and it survives a Worker
restart. Then call the three activities in sequence.

## Part C: Compare the two paths

```bash
# Terminal 1
gradle runWorker

# Terminal 2
gradle runStarter     # ORD-1001, under the limit
gradle runDeclined    # ORD-1002, over the limit
```

In `fulfillment-ORD-1001` there is **no record of the two failed attempts**. One
`ActivityTaskScheduled`, one `ActivityTaskStarted` — and the started event says
`attempt: 3`. Retries don't append to history; history records the outcome and
which attempt produced it.

In `fulfillment-ORD-1002`, `ProcessPayment` has an `ActivityTaskFailed` event
carrying `retryState: RETRY_STATE_NON_RETRYABLE_FAILURE`, and
`DispatchToFulfillment` never ran. A terminal failure *is* recorded.

One activity failed twice and the workflow shrugged; the other failed once and the
workflow stopped. The only difference is which `ApplicationFailure` was chosen.

**Try this:** kill the Worker (Ctrl+C) while `reserveInventory` is retrying, then
restart it. The workflow resumes mid-retry. The pipeline couldn't — its retry state
was a local variable.

### This is the end of the exercise.
