## Exercise 2: Child Workflows

During this exercise, you will:

- Extract inventory reservation into a dedicated child workflow
- Understand when child workflows help (isolation, separate history, retry boundaries)
- Call a child workflow from a parent using `Workflow.newChildWorkflowStub()`

## Setup

```bash
temporal server start-dev
```

`cd` into `2_child_workflows/practice/`.

## Part A: Create the WarehouseActivities stub

Open `InventoryReservationWorkflowImpl.kt` and create a `WarehouseActivities` stub
with a 30-second `StartToCloseTimeout`.

## Part B: Implement the child workflow logic

In `reserve()`, iterate over `WAREHOUSES` and call `checkWarehouseInventory` for each.
Return the first non-null result. If all return null, throw a non-retryable
`ApplicationFailure` with type `"OutOfStock"`.

## Part C: Call the child from the parent

Open `FulfillmentWorkflowImpl.kt`. Use `Workflow.newChildWorkflowStub(...)` with
`ChildWorkflowOptions` that set `workflowId = "inventory-${order.orderId}"`.

## Part D: Run It

```bash
# Terminal 1
gradle runWorker

# Terminal 2
gradle runStarter
```

In the Web UI, find both `fulfillment-ORD-1002` and `inventory-ORD-1002`.
Notice the child has its own separate Event History.

### This is the end of the exercise.
