## Exercise 1: Converting a Workflow

During this exercise, you will:

- Identify the problems with manual retry loops and local variable state
- See that Temporal retries failed Activities by default, so you inherit retry
- Recognise the failures that should NOT be retried, and opt out of them
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

## Part A: Two Activities

`dispatchToFulfillment` is already written — read it first. Then:

- **`reserveInventory`** — call `WarehouseClient.reserve(...)` and return it. That's
  the whole method. The client times out on the first two attempts (see
  `WarehouseClient.kt`); let the exception propagate. Temporal retries failed
  Activities by default, which is right for a transient failure.
- **`processPayment`** — decline over `CREDIT_LIMIT`, using
  `ApplicationFailure.newFailure(...)` for now. That's deliberately the wrong
  choice; Part D is where you see why.

## Part B: The Workflow

Create the stub with a 30-second `StartToCloseTimeout` and `RetryOptions` of
`maximumAttempts(5)` / `initialInterval(2s)` — the same policy as the pipeline's
`MAX_RETRIES` and `RETRY_DELAY_MS`, except Temporal does the waiting and it
survives a Worker restart. Then call the three activities in sequence.

## Part C: Retry you got for free

```bash
gradle runWorker      # terminal 1
gradle runStarter     # terminal 2
```

The warehouse times out twice and succeeds on the third attempt. You wrote no retry
code. In `fulfillment-ORD-1001` there is **no record of the two failures** — one
`ActivityTaskScheduled`, one `ActivityTaskStarted` saying `attempt: 3`. History
records the outcome and which attempt produced it, not every try.

## Part D: Retry you didn't want

```bash
gradle runDeclined    # ORD-1002, over the limit
```

Watch it decline the same card five times across ~30s of backoff (~40s for the
whole run). That's the
pipeline's bug reproduced: `newFailure` means "worth retrying," and a decline never
is. Switch to `newNonRetryableFailure`, restart the Worker, run it again — it fails
instantly, and `ProcessPayment` carries
`retryState: RETRY_STATE_NON_RETRYABLE_FAILURE` while `DispatchToFulfillment` never
runs.

**Try this:** kill the Worker (Ctrl+C) while the warehouse is still timing out, then
restart it. The workflow resumes mid-retry. The pipeline couldn't — its retry state
was a local variable.

### This is the end of the exercise.
