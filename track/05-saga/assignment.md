---
slug: saga
id: oozxewo2hysx
type: challenge
title: 'Exercise 5: The Saga Pattern'
teaser: Implement compensating transactions so multi-step workflows clean up after
  themselves on failure.
notes:
- type: text
  contents: |-
    Distributed systems can't use a single database transaction to span multiple
    services. When your workflow calls three external APIs in sequence — reserve
    inventory, charge payment, dispatch shipment — and the third one fails, the
    first two have already committed.

    The **Saga pattern** solves this with **compensating transactions**: each
    forward step has a corresponding undo step. If the pipeline breaks, the
    workflow runs the compensations in reverse order, restoring consistent state.

    In Temporal this is plain Java: wrap the forward steps in `try/catch`.
    Declare your state variables (`reservationId`, `paymentConfirmation`) before
    the `try` block so the `catch` can see them. In the `catch`, call
    compensating activities only for the steps that actually completed — guard
    each call on null.

    Because workflow execution is durable, compensations will run to completion
    even if the Worker restarts mid-compensation.

    Hit **Start** when you're ready.
tabs:
- id: tmprzixga4ng
  title: VS Code
  type: service
  hostname: workshop-host
  path: ?folder=/workspace/exercise&openFile=/workspace/exercise/src/main/java/fulfillment/FulfillmentWorkflowImpl.java
  port: 8443
- id: 95qdga1qhrae
  title: Terminal 1 - Worker
  type: terminal
  hostname: workshop-host
  workdir: /workspace/exercise
- id: xfu3ljncq7ku
  title: Terminal 2 - Starter
  type: terminal
  hostname: workshop-host
  workdir: /workspace/exercise
- id: qslyvye3rsmg
  title: Temporal Web UI
  type: service
  hostname: workshop-host
  path: /
  port: 8080
difficulty: intermediate
timelimit: 2700
enhanced_loading: null
---

## Exercise 5: The Saga Pattern

All your work is in **`FulfillmentWorkflowImpl.java`** (active tab).

Files are in `/workspace/exercise/src/main/java/fulfillment/`.
Look for the two `// TODO` blocks.

***

### Part A – Understand the domain

Open `FulfillmentActivities.java` (second tab). Notice it has five methods:

**Forward steps** (run in order):
1. `reserveInventory` — holds stock for the order
2. `processPayment` — charges the customer
3. `dispatchToFulfillment` — hands off to the warehouse

**Compensating transactions** (run in reverse if something fails):
- `refundPayment` — undoes `processPayment`
- `releaseInventory` — undoes `reserveInventory`

There is no compensation for `dispatchToFulfillment` — if dispatch fails, it
never completed, so there's nothing to undo there.

***

### Part B – Wrap the forward steps in try/catch

The state variables `reservationId` and `paymentConfirmation` are already
declared as `null` before the `try`. Inside the `try` block, call all three
forward steps and return on success:

```java
reservationId = activities.reserveInventory(order);
paymentConfirmation = activities.processPayment(order);
String trackingNumber = activities.dispatchToFulfillment(order, reservationId);
return new OrderResult(order.getOrderId(), "FULFILLED",
    reservationId, paymentConfirmation, trackingNumber);
```

Remove the `NOT_IMPLEMENTED` placeholder return.

***

### Part C – Compensate in reverse order

In the `catch (Exception e)` block, undo only the steps that completed.
The null guards are the key — if `reserveInventory` threw before returning,
`reservationId` is still `null`, so there's nothing to release:

```java
if (paymentConfirmation != null) {
    activities.refundPayment(paymentConfirmation);
}
if (reservationId != null) {
    activities.releaseInventory(reservationId);
}
return new OrderResult(order.getOrderId(), "FAILED",
    reservationId, paymentConfirmation, null);
```

***

### Part D – Run and observe

1. Click the [button label="Terminal 1 - Worker" background="#444CE7"](tab-1) tab and start the Worker:

   ```bash,run
   mvn compile exec:java -Dexec.mainClass="fulfillment.FulfillmentWorker"
   ```

2. Click the [button label="Terminal 2 - Starter" background="#444CE7"](tab-2) tab and run the Starter:

   ```bash,run
   mvn exec:java -Dexec.mainClass="fulfillment.Starter"
   ```

`dispatchToFulfillment` fails 30% of the time. Run the Starter a few times until
you see a failure. In the [button label="Temporal Web UI" background="#444CE7"](tab-3), open `fulfillment-ORD-1005` and look at the
Event History — you should see `refundPayment` and `releaseInventory` activity
events immediately following the dispatch failure.

**Discussion:** Where does the Saga pattern apply in your own workflows? Think
about: payment + shipping + notification pipelines, multi-service onboarding
flows, reservation + pricing + booking steps.

***

Click **Check** when done, or **Solve** to see the reference solution.
