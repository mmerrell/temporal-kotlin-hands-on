---
slug: converting
id: byvjpgcuvsa4
type: challenge
title: 'Exercise 1: Converting a Workflow'
teaser: Replace a fragile retry loop with Temporal Activities and a durable Workflow.
notes:
- type: text
  contents: |-
    The starting point for this exercise is `FulfillmentPipeline.java` — a deliberately fragile
    implementation using `Thread.sleep()` retries and local variable state.

    If the process dies mid-execution, the order is lost. If a step fails after three attempts,
    the whole pipeline crashes. Retry behavior is hardcoded and invisible.

    **Temporal fixes all of this.** Your job is to move the logic into proper
    Activity implementations and wire them together in a Workflow.

    Hit **Start** when you're ready.
tabs:
- id: beatbahvb8mh
  title: VS Code
  type: service
  hostname: workshop-host
  path: ?folder=/workspace/exercise&openFile=/workspace/exercise/src/main/java/fulfillment/FulfillmentPipeline.java&openFile=/workspace/exercise/src/main/java/fulfillment/FulfillmentActivitiesImpl.java
  port: 8443
- id: aucxy2upmgp6
  title: Terminal 1 - Worker
  type: terminal
  hostname: workshop-host
  workdir: /workspace/exercise
- id: owpxr5lnaa1c
  title: Terminal 2 - Starter
  type: terminal
  hostname: workshop-host
  workdir: /workspace/exercise
- id: beuekqadj4dh
  title: Temporal Web UI
  type: service
  hostname: workshop-host
  path: /
  port: 8080
difficulty: basic
timelimit: 2400
enhanced_loading: null
---

## Exercise 1: Converting a Workflow

Open **`FulfillmentActivitiesImpl.java`** and **`FulfillmentWorkflowImpl.java`** in VS Code.
Look for `// TODO` comments — they mark everything you need to implement.

> **Note:** Wait for the "Java" indicator in the VS Code status bar to finish indexing before
> expecting autocomplete and the Problems panel to work. This takes about 30-60 seconds on first load.

Files are in `/workspace/exercise/src/main/java/fulfillment/`.

***

### Part A – Implement the three Activities

In `FulfillmentActivitiesImpl.java`, fill in each of the three methods.
Look at `FulfillmentPipeline.java` (already open in a tab) to understand what each method should do.

For each method:
- Replace the `throw new UnsupportedOperationException(...)` stub with the actual logic
- Replace raw `throw new Exception(...)` calls with `ApplicationFailure.newFailure(message, type)`
- Keep the `Math.random()` failure simulation — Temporal will retry it automatically

***

### Part B – Implement the Workflow

In `FulfillmentWorkflowImpl.java`:

1. Create `ActivityOptions` with a `StartToCloseTimeout` of **30 seconds**
2. Create a `FulfillmentActivities` stub via `Workflow.newActivityStub(...)`
3. Replace the `null` stub for `activities`
4. In `processOrder()`, call the three activities in sequence and return an `OrderResult`

***

### Part C – Run it

Once your code compiles, start both processes:

1. Click the [button label="Terminal 1 - Worker" background="#444CE7"](tab-1) tab and start the Worker:

   ```bash,run
   mvn compile exec:java -Dexec.mainClass="fulfillment.FulfillmentWorker"
   ```

2. Click the [button label="Terminal 2 - Starter" background="#444CE7"](tab-2) tab and run the Starter:

   ```bash,run
   mvn exec:java -Dexec.mainClass="fulfillment.Starter"
   ```

Open the [button label="Temporal Web UI" background="#444CE7"](tab-3) tab and find workflow `fulfillment-ORD-1001`.
Try killing the Worker mid-execution (Ctrl+C in Terminal 1) and restarting it — what happens?

***

Click **Check** when you think you're done. The checker will verify your source code and confirm the project compiles.

> **Stuck?** Click **Solve** to apply the reference solution and see a working implementation.
