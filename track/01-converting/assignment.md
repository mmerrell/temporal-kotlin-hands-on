---
slug: converting
id: 8m9lheaemgnm
type: challenge
title: 'Exercise 1: Converting a Workflow'
teaser: Replace a fragile retry loop with Temporal Activities and a durable Workflow.
notes:
- type: text
  contents: |-
    `FulfillmentPipeline.kt` is the "before Temporal" version of an order pipeline:
    reserve inventory, take payment, dispatch. Ninety-five lines, and about sixty of
    them are retry bookkeeping — three near-identical loops, a `MAX_RETRIES`
    constant, `Thread.sleep()` between attempts, and state in local variables that
    vanishes if the JVM dies.

    Read its comments before you start. Two of them admit real bugs: a reservation
    that can never be rolled back, and a customer who can be charged for an order
    that never ships.

    There's a subtler problem too. The pipeline treats every failure identically.
    A warehouse timeout worth retrying and a declined card that will never succeed
    both get five attempts and four sleeps. Telling those two apart is your job in
    this exercise, and it's the part Temporal can't decide for you.

    Hit **Start** when you're ready.
tabs:
- id: ipj9uh36unsk
  title: Code Editor
  type: code
  hostname: workshop
  path: /workspace/exercise
- id: gzyxqvndjmd5
  title: Terminal 1 - Worker
  type: terminal
  hostname: workshop
  workdir: /workspace/exercise
- id: cx2322ivai8l
  title: Terminal 2 - Starter
  type: terminal
  hostname: workshop
  workdir: /workspace/exercise
- id: umq858v8stst
  title: Temporal Web UI
  type: service
  hostname: workshop
  path: /
  port: 8080
difficulty: basic
timelimit: 2400
enhanced_loading: null
---

## Exercise 1: Converting a Workflow

Your work is in **`FulfillmentActivitiesImpl.kt`** and **`FulfillmentWorkflowImpl.kt`**.
`FulfillmentPipeline.kt` is the code you're replacing — read it first.

> The Code Editor gives you syntax highlighting but no autocomplete or inline errors.
> Run `gradle compileKotlin` in a terminal whenever you want to check your work.

***

### Part A – Two Activities, two kinds of failure

`dispatchToFulfillment` **is already written** at the bottom of
`FulfillmentActivitiesImpl.kt`. Read it first — it shows the shape. Then write the
other two. They fail for genuinely different reasons, and that difference is the
whole point.

**A1 — `reserveInventory`.** The warehouse service times out under load. That's
transient: the same call a moment later usually works. Make the failure **retryable**:

```kotlin
throw ApplicationFailure.newFailure("Warehouse service timed out", "InventoryError")
```

To make the retry visible, fail the first two attempts and succeed on the third.
An Activity can see which attempt it's on:

```kotlin
val attempt = Activity.getExecutionContext().info.attempt
```

**A2 — `processPayment`.** The card is declined because the order exceeds
`CREDIT_LIMIT`. Retrying a declined card declines it again, five times, slower.
Make that failure **non-retryable**:

```kotlin
throw ApplicationFailure.newNonRetryableFailure(
    "Card declined: ...", "PaymentDeclined")
```

Temporal will do exactly what you tell it, including retrying something pointless
thirty times. Choosing correctly here is the actual skill.

***

### Part B – The Workflow

In `FulfillmentWorkflowImpl.kt`, replace the `null` stub:

```kotlin
private val activities: FulfillmentActivities = Workflow.newActivityStub(
    FulfillmentActivities::class.java,
    ActivityOptions.newBuilder()
        .setStartToCloseTimeout(Duration.ofSeconds(30))
        .setRetryOptions(
            RetryOptions.newBuilder()
                .setInitialInterval(Duration.ofSeconds(2))
                .setMaximumAttempts(5)
                .build()
        )
        .build()
)
```

Compare those `RetryOptions` with `MAX_RETRIES = 5` and `RETRY_DELAY_MS = 2000` in
`FulfillmentPipeline.kt`. Identical policy — except Temporal does the waiting, and
the retry survives the Worker being killed halfway through it.

Then call the three activities in sequence and return an `OrderResult`:

```kotlin
val reservationId = activities.reserveInventory(order)
val paymentConfirmation = activities.processPayment(order)
val trackingNumber = activities.dispatchToFulfillment(order, reservationId)
```

Note what you did *not* write: no try/catch, no loop, no `Thread.sleep`, no attempt
counter. That's the sixty lines.

***

### Part C – The retryable failure

Start the Worker in [button label="Terminal 1 - Worker" background="#444CE7"](tab-1):

```bash,run
gradle runWorker
```

Then in [button label="Terminal 2 - Starter" background="#444CE7"](tab-2):

```bash,run
gradle runStarter
```

Watch the Worker terminal: `reserveInventory` logs attempt 1, attempt 2, attempt 3,
with a pause between each. The order completes.

Now open `fulfillment-ORD-1001` in the
[button label="Temporal Web UI" background="#444CE7"](tab-3) and count the events.
**There is no record of the two failures.** You'll find one
`ActivityTaskScheduled` for `ReserveInventory`, one `ActivityTaskStarted` — and that
started event says `attempt: 3`.

Retries don't append to history. History records what happened and which attempt
managed it, not every try along the way. That's why a heavily-retried Activity
doesn't bloat your history.

***

### Part D – The non-retryable failure

Same code, an order over the credit limit:

```bash,run
gradle runDeclined
```

The Starter prints a failure instead of a result. Open `fulfillment-ORD-1002` and
compare it with the run before:

- `ReserveInventory` retried and succeeded, exactly as before
- `ProcessPayment` has an **`ActivityTaskFailed`** event — a terminal failure *is*
  recorded — carrying `retryState: RETRY_STATE_NON_RETRYABLE_FAILURE`
- `DispatchToFulfillment` never ran at all

One activity failed twice and the workflow shrugged. The other failed once and the
workflow stopped. The only difference is which `ApplicationFailure` you chose.

**Then try this:** start another order and kill the Worker (Ctrl+C in Terminal 1)
while `reserveInventory` is still retrying. Restart it with `gradle runWorker`. The
workflow picks up mid-retry and finishes. The pipeline you deleted could not do
that — its retry state lived in a local variable.

***

Click **Check** when done, or **Solve** to see the reference solution.
