---
slug: parallel-activities
id: frwdpf5xe5aj
type: challenge
title: 'Exercise 3: Parallel Activities'
teaser: Fan out warehouse checks concurrently with Async.function() and Promise.allOf().
notes:
- type: text
  contents: |-
    In Exercise 2, the inventory child workflow checks warehouses **one at a time**.
    If each `checkWarehouseInventory` call takes 500ms and you have 6 warehouses,
    that's 3 seconds minimum — even if the first warehouse has stock.

    **The parallel pattern checks all warehouses simultaneously.**
    At scale, this isn't an optimization — it's a requirement.
    The same fan-out pattern applies anywhere you're processing many independent
    items: containers, SKUs, payment methods, notification channels.

    The Temporal SDK is **synchronous by default** — you have to explicitly opt in
    to concurrency using `Async.function()`. This makes concurrent code readable
    and keeps it deterministic for replay.

    Hit **Start** when you're ready.
tabs:
- id: lbucwqwjcjq0
  title: Code Editor
  type: code
  hostname: workshop
  path: /workspace/exercise
- id: gapb6ymsevew
  title: Terminal 1 - Worker
  type: terminal
  hostname: workshop
  workdir: /workspace/exercise
- id: 6cwewethblmm
  title: Terminal 2 - Starter
  type: terminal
  hostname: workshop
  workdir: /workspace/exercise
- id: fnwqrqyydiwz
  title: Temporal Web UI
  type: service
  hostname: workshop
  path: /
  port: 8080
difficulty: intermediate
timelimit: 2400
enhanced_loading: null
---

## Exercise 3: Parallel Activities

All your work is in **`InventoryReservationWorkflowImpl.kt`** (active tab).
The activity stub is already wired up — focus on the `reserve()` method.

Files are in `/workspace/exercise/src/main/kotlin/fulfillment/`.

***

### Part A – Fan out with Async.function()

For each `warehouseId` in `WAREHOUSES`, launch the activity concurrently:

```kotlin
val promises: List<Promise<String>> = WAREHOUSES.map { warehouseId ->
    Async.function(
        warehouseActivities::checkWarehouseInventory,
        warehouseId, sku, quantity
    )
}
```

`Async.function()` schedules the activity without blocking here.
All six `checkWarehouseInventory` calls are in-flight simultaneously.

***

### Part B – Wait with Promise.allOf()

```kotlin
Promise.allOf(promises).get()
```

This blocks the workflow durably until **every** promise resolves.
If the worker restarts mid-wait, Temporal replays the promises from history.

***

### Part C – Return the first success

```kotlin
for (p in promises) {
    val result = p.get()
    if (result != null) {
        return result
    }
}
throw ApplicationFailure.newNonRetryableFailure("No stock available", "OutOfStock")
```

***

### Part D – Run it and compare

1. Click the [button label="Terminal 1 - Worker" background="#444CE7"](tab-1) tab and start the Worker:

   ```bash,run
   gradle runWorker
   ```

2. Click the [button label="Terminal 2 - Starter" background="#444CE7"](tab-2) tab and run the Starter:

   ```bash,run
   gradle runStarter
   ```

In the [button label="Temporal Web UI" background="#444CE7"](tab-3), open `inventory-ORD-1003`. Look at the Event History —
all six `ActivityTaskScheduled` events appear nearly simultaneously.
Compare with Exercise 2 where they were staggered.

***

Click **Check** when done, or **Solve** to see the reference solution.
