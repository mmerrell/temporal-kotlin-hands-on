---
slug: cost-optimization
id: tj5ep5lf8mne
type: challenge
title: 'Exercise 4: Cost Optimization with Local Activities'
teaser: Move fast in-process steps to Local Activities to cut Temporal Cloud Actions.
notes:
- type: text
  contents: |-
    In Temporal Cloud, pricing is based on **Actions** — each interaction between
    a Worker and the Temporal Server. A regular activity generates **three Actions**:

    1. `ActivityTaskScheduled` — server records the intent
    2. `ActivityTaskStarted` — worker picks it up
    3. `ActivityTaskCompleted` — worker reports the result

    For steps that run fast and in-process — input validation, fraud rules, config lookups,
    format conversion — this round-trip is unnecessary overhead, both in latency and cost.

    **Local Activities** run directly inside the Worker process. They produce a single
    `MarkerRecorded` event instead of the three-event triplet.
    Same durability guarantee. One-third the Actions cost.

    The tradeoff: local activities can't be cancelled externally, and a long-running local
    activity blocks the workflow thread. Use them for **fast, non-network steps only**.

    Hit **Start** when you're ready.
tabs:
- id: pglzy9kbfevz
  title: VS Code
  type: service
  hostname: workshop
  path: ?folder=/workspace/exercise&openFile=/workspace/exercise/src/main/java/fulfillment/LocalFulfillmentActivities.java&openFile=/workspace/exercise/src/main/java/fulfillment/FulfillmentWorkflowImpl.java
  port: 8443
- id: 7nuetgdclb5n
  title: Terminal 1 - Worker
  type: terminal
  hostname: workshop
  workdir: /workspace/exercise
- id: eeosujydpw6m
  title: Terminal 2 - Starter
  type: terminal
  hostname: workshop
  workdir: /workspace/exercise
- id: xxngxgmvz5mw
  title: Temporal Web UI
  type: service
  hostname: workshop
  path: /
  port: 8080
difficulty: intermediate
timelimit: 2400
enhanced_loading: null
---

## Exercise 4: Cost Optimization with Local Activities

All your work is in **`FulfillmentWorkflowImpl.java`** (active tab).
`LocalFulfillmentActivities.java` is open in a second tab — review it to see what methods are available.

Files are in `/workspace/exercise/src/main/java/fulfillment/`.
Look for the two `// TODO` blocks — one at the field declaration, one inside `processOrder()`.

***

### Part A – Create the Local Activity stub

Replace the `null` stub with a real `LocalFulfillmentActivities` stub.

The key differences from a regular activity stub:
- Use `LocalActivityOptions` (not `ActivityOptions`)
- Use `Workflow.newLocalActivityStub()` (not `Workflow.newActivityStub()`)

```java
private final LocalFulfillmentActivities localActivities = Workflow.newLocalActivityStub(
    LocalFulfillmentActivities.class,
    LocalActivityOptions.newBuilder()
        .setStartToCloseTimeout(Duration.ofSeconds(5))
        .build()
);
```

A 5-second timeout is appropriate — if `validateOrder` or `fraudCheck` take longer
than 5 seconds, something is wrong.

***

### Part B – Call local activities first

Add these two calls at the top of `processOrder()`, **before** the child workflow invocation:

```java
localActivities.validateOrder(order);
localActivities.fraudCheck(order);
```

These run synchronously in the Worker process — no round-trip to the Temporal Server.

***

### Part C – Compare Event Histories

1. Click the [button label="Terminal 1 - Worker" background="#444CE7"](tab-1) tab and start the Worker:

   ```bash,run
   mvn compile exec:java -Dexec.mainClass="fulfillment.FulfillmentWorker"
   ```

2. Click the [button label="Terminal 2 - Starter" background="#444CE7"](tab-2) tab and run the Starter:

   ```bash,run
   mvn exec:java -Dexec.mainClass="fulfillment.Starter"
   ```

In the [button label="Temporal Web UI" background="#444CE7"](tab-3), open `fulfillment-ORD-1004` and inspect the Event History.

Look for:
- `MarkerRecorded` — one event each for `validateOrder` and `fraudCheck` (local)
- `ActivityTaskScheduled / Started / Completed` triplets — for `processPayment` and `dispatchToFulfillment` (remote)

**Discussion:** What steps in your own workflows could be local activities?
Think about: input validation, in-memory lookups, format conversion, fraud rules, config from a local cache.

***

Click **Check** when done, or **Solve** to see the reference solution.
