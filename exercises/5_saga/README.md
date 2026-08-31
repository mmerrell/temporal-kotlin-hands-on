## Exercise 5: The Saga Pattern

During this exercise, you will:

- Wrap a multi-step workflow in try/catch to detect failures
- Run compensating activities in reverse order when any step fails
- Observe the compensation events in the Temporal Web UI event history

The **Saga pattern** solves a fundamental problem in distributed systems: you
can't wrap calls to three separate services in a single database transaction.
Instead, each step that succeeds must have a corresponding *compensating
transaction* that can undo it if a later step fails.

In this exercise the order pipeline has three forward steps:

1. Reserve inventory
2. Process payment
3. Dispatch to fulfillment

If dispatch fails after payment succeeded, the workflow must refund the payment
and release the inventory reservation — in that reverse order.

## Setup

```bash
temporal server start-dev
```

`cd` into `5_saga/practice/`.

## Part A: Understand the domain

Open `FulfillmentActivities.kt`. It has five methods:

**Forward steps:** `reserveInventory`, `processPayment`, `dispatchToFulfillment`

**Compensating transactions:** `refundPayment` (undoes payment), `releaseInventory` (undoes reservation)

There is no compensation for `dispatchToFulfillment` — if it failed, it never
completed, so there's nothing to undo.

## Part B: Wrap the forward steps in try/catch

In `FulfillmentWorkflowImpl.kt`, replace the placeholder with the three
activity calls inside a `try` block. The state variables `reservationId` and
`paymentConfirmation` are already declared as `null` before the `try` — this is
intentional so the `catch` block can read them.

```kotlin
reservationId = activities.reserveInventory(order)
paymentConfirmation = activities.processPayment(order)
val trackingNumber = activities.dispatchToFulfillment(order, reservationId)
return OrderResult(order.orderId, "FULFILLED",
    reservationId, paymentConfirmation, trackingNumber)
```

## Part C: Compensate in reverse order

In `catch (e: Exception)`, run compensating activities only for the steps that
completed. Guard each call on null — if a step never ran, the variable is still
`null` and there's nothing to undo:

```kotlin
if (paymentConfirmation != null) {
    activities.refundPayment(paymentConfirmation)
}
if (reservationId != null) {
    activities.releaseInventory(reservationId)
}
return OrderResult(order.orderId, "FAILED", reservationId, paymentConfirmation, null)
```

## Part D: Run It and Observe

```bash
# Terminal 1
gradle runWorker

# Terminal 2
gradle runStarter
```

`dispatchToFulfillment` has a 30% failure rate. Run the Starter a few times
until you catch a failure. In the Web UI, open `fulfillment-ORD-1005` and
inspect the Event History. Look for `refundPayment` and `releaseInventory`
activity events following the dispatch failure.

**Discussion:** Where does the Saga pattern apply in your own workflows?
Think about: payment + shipping + notification pipelines, multi-service
onboarding flows, reservation + pricing + booking steps.

### This is the end of the exercise.

---
