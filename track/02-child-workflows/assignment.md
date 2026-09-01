---
slug: child-workflows
id: pmicrhafgcdn
type: challenge
title: 'Exercise 2: Child Workflows'
teaser: Decompose inventory reservation into a dedicated child workflow with its own
  history.
notes:
- type: text
  contents: |-
    In Exercise 1, inventory reservation was just an activity in the main workflow.
    That works, but it means the reservation logic shares history with payment and dispatch.

    **Child workflows give you:**
    - A **separate Event History** — the parent's history stays lean
    - An **independent retry boundary** — the child can fail and be retried without restarting the parent
    - A **meaningful workflow ID** you can query or signal independently (e.g., `inventory-ORD-1002`)

    This pattern is common at companies running Temporal at scale — complex sub-processes
    become first-class workflows rather than buried activity chains.

    Hit **Start** when you're ready.
tabs:
- id: ubv2xvpspuuf
  title: Code Editor
  type: code
  hostname: workshop
  path: /workspace/exercise
- id: tuwuwbyk1ydt
  title: Terminal 1 - Worker
  type: terminal
  hostname: workshop
  workdir: /workspace/exercise
- id: vk6xkdtczhfr
  title: Terminal 2 - Starter
  type: terminal
  hostname: workshop
  workdir: /workspace/exercise
- id: eklmxe0t8hv6
  title: Temporal Web UI
  type: service
  hostname: workshop
  path: /
  port: 8080
difficulty: basic
timelimit: 3600
enhanced_loading: null
---

## Exercise 2: Child Workflows

You're working with two implementation files this time:
- **`InventoryReservationWorkflowImpl.kt`** — the child workflow (new, active tab)
- **`FulfillmentWorkflowImpl.kt`** — the parent workflow (calls the child)

Files are in `/workspace/exercise/src/main/kotlin/fulfillment/`. Look for `TODO(...)` markers in both.

***

### Part A – Create the WarehouseActivities stub

In `InventoryReservationWorkflowImpl.kt`, replace the `TODO(...)` on the
`warehouseActivities` line. Edit it in place — a second declaration won't compile:

```kotlin
private val warehouseActivities: WarehouseActivities = Workflow.newActivityStub(
    WarehouseActivities::class.java,
    ActivityOptions.newBuilder()
        .setStartToCloseTimeout(Duration.ofSeconds(30))
        .build()
)
```

> **Note:** Each warehouse check simulates a 5-second network call, and stock is only
> available at the 5th and 6th warehouses. Serial search takes ~25 seconds — that's intentional.
> Watch the Web UI while it runs and notice each warehouse being checked one at a time
> (Chicago → Santiago → Amsterdam → Nairobi → Singapore → Sydney).
> Exercise 3 will fix this.

***

### Part B – Implement the child workflow logic

In `reserve()`, iterate over `WAREHOUSES` and call `checkWarehouseInventory` for each.
Return the **first non-null** result (first warehouse with stock wins).

If all warehouses return null, throw a **non-retryable** `ApplicationFailure`:
```kotlin
throw ApplicationFailure.newNonRetryableFailure("No stock available", "OutOfStock")
```

***

### Part C – Call the child from the parent

In `FulfillmentWorkflowImpl.kt`, replace the `TODO(...)` stub for `reservationId`:
```kotlin
val inventoryWorkflow = Workflow.newChildWorkflowStub(
    InventoryReservationWorkflow::class.java,
    ChildWorkflowOptions.newBuilder()
        .setWorkflowId("inventory-${order.orderId}")
        .build()
)
val reservationId = inventoryWorkflow.reserve(order.itemSku, order.quantity)
```

***

### Part D – Run it

1. Click the [button label="Terminal 1 - Worker" background="#444CE7"](tab-1) tab and start the Worker:

   ```bash,run
   gradle runWorker
   ```

2. Click the [button label="Terminal 2 - Starter" background="#444CE7"](tab-2) tab and run the Starter:

   ```bash,run
   gradle runStarter
   ```

In the [button label="Temporal Web UI" background="#444CE7"](tab-3), find both `fulfillment-ORD-1002` and `inventory-ORD-1002`.
Click into each — notice the child has its own separate Event History.

***

Click **Check** when done, or **Solve** to see the reference solution.
