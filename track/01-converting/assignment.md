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

    There's a subtler problem, and it's the one this exercise is about. The pipeline
    treats every failure identically. A warehouse timeout worth retrying and a
    declined card that will never succeed both get five attempts and four sleeps.

    Temporal retries failed Activities automatically, so it makes that same mistake
    by default — unless you tell it which failures aren't worth retrying. That
    judgment is the part no framework can make for you.

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
Read `FulfillmentPipeline.kt` first — it's the code you're replacing.

> The Code Editor gives you syntax highlighting but no autocomplete or inline errors.
> Run `gradle compileKotlin` in a terminal whenever you want to check your work.

***

### Part A – Two Activities

`dispatchToFulfillment` **is already written** at the bottom of
`FulfillmentActivitiesImpl.kt`. Read it first — it shows the shape.

**A1 — `reserveInventory`.** Call the warehouse and return the reservation:

```kotlin
log.info("Reserving {} x {} for order {}", order.quantity, order.itemSku, order.orderId)
return WarehouseClient.reserve(order.itemSku, order.quantity, order.orderId)
```

That's the whole method. `WarehouseClient` is a stand-in for the warehouse's API,
and like a lot of real ones it times out under load — open `WarehouseClient.kt` and
you'll see it fail the first two attempts before succeeding.

**Do not catch that exception.** Let it propagate. You aren't adding retry
behaviour; you're inheriting it, and for a transient infrastructure failure that's
the right answer.

**A2 — `processPayment`.** Decline anything over `CREDIT_LIMIT`:

```kotlin
log.info("Charging {} to customer {}", order.totalAmount, order.customerId)

if (order.totalAmount > CREDIT_LIMIT) {
    throw ApplicationFailure.newFailure(
        "Card declined: order total ${order.totalAmount} exceeds credit limit $CREDIT_LIMIT",
        "PaymentDeclined")
}

return "PAY-${order.orderId}"
```

Write it exactly like that, with `newFailure`. It's the wrong choice. Part D is
where you find out why — leave it alone until then.

***

### Part B – The Workflow

In `FulfillmentWorkflowImpl.kt`, replace the `TODO(...)` on the `activities` line.
Edit that line in place — a second `activities` declaration won't compile:

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

### Part C – Retry you got for free

Start the Worker in [button label="Terminal 1 - Worker" background="#444CE7"](tab-1):

```bash,run
gradle runWorker
```

Then in [button label="Terminal 2 - Starter" background="#444CE7"](tab-2):

```bash,run
gradle runStarter
```

Watch the Worker log the warehouse call on attempt 1, attempt 2, attempt 3, pausing
between each. The order completes. You wrote no retry code at all.

Now open `fulfillment-ORD-1001` in the
[button label="Temporal Web UI" background="#444CE7"](tab-3) and count the events.
**The two failures aren't there.** One `ActivityTaskScheduled` for
`ReserveInventory`, one `ActivityTaskStarted` — and that started event says
`attempt: 3`.

Retries don't append to history. History records what happened and which attempt
managed it, not every try along the way. A heavily-retried Activity doesn't bloat
your history.

***

### Part D – Retry you didn't want

Now the order that gets declined:

```bash,run
gradle runDeclined
```

**Watch the Worker terminal and wait.** The card is declined, and Temporal does
what you told it to: it charges again. And again. Five charge attempts across about
thirty seconds of backoff — the whole run takes roughly forty seconds — every one of
them declined for exactly the reason it was declined the first time.

Count the `Charging ...` lines in the Worker log. There will be five.

That's the pipeline's bug, faithfully reproduced. `newFailure` means "worth
retrying," and a declined card never is.

Fix it — one call in `processPayment`:

```kotlin
throw ApplicationFailure.newNonRetryableFailure(
    "Card declined: order total ${order.totalAmount} exceeds credit limit $CREDIT_LIMIT",
    "PaymentDeclined")
```

Restart the Worker and run `gradle runDeclined` again. It fails instantly.

Open `fulfillment-ORD-1002` and compare against the first run:

- `ReserveInventory` retried and succeeded, exactly as before — that failure is
  still retryable, and still should be
- `ProcessPayment` has one `ActivityTaskFailed` carrying
  `retryState: RETRY_STATE_NON_RETRYABLE_FAILURE`
- `DispatchToFulfillment` never ran

Two activities, two kinds of failure, one word of difference. Temporal's default is
to retry; deciding what shouldn't is your job.

**One more thing:** start another order and kill the Worker (Ctrl+C in Terminal 1)
while the warehouse is still timing out. Restart it with `gradle runWorker`. The
workflow picks up mid-retry and finishes. The pipeline you deleted couldn't — its
retry state was a local variable.

***

Click **Check** when done, or **Solve** to see the reference solution.
