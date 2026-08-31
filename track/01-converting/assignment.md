---
slug: converting
id: 8m9lheaemgnm
type: challenge
title: 'Exercise 1: Converting a Workflow'
teaser: Replace a fragile retry loop with Temporal Activities and a durable Workflow.
notes:
- type: text
  contents: |-
    The starting point for this exercise is `FulfillmentPipeline.kt` — a deliberately fragile
    implementation using `Thread.sleep()` retries and local variable state.

    If the process dies mid-execution, the order is lost. If a step fails after three attempts,
    the whole pipeline crashes. Retry behavior is hardcoded and invisible.

    **Temporal fixes all of this.** Your job is to move the logic into proper
    Activity implementations and wire them together in a Workflow.

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

Open **`FulfillmentActivitiesImpl.kt`** and **`FulfillmentWorkflowImpl.kt`** in the Code Editor tab.
Look for `TODO(...)` markers — they mark everything you need to implement.

> **Note:** The Code Editor gives you syntax highlighting but not autocomplete or inline
> error checking — use the terminal to run `gradle compileKotlin` any time you want to
> check your work before clicking Check.

Files are in `/workspace/exercise/src/main/kotlin/fulfillment/`.

***

### Part A – Implement the three Activities

In `FulfillmentActivitiesImpl.kt`, fill in each of the three methods.
Look at `FulfillmentPipeline.kt` (already open in a tab) to understand what each method should do.

For each method:
- Replace the `TODO(...)` stub with the actual logic
- Replace raw `throw Exception(...)` calls with `ApplicationFailure.newFailure(message, type)`
- Keep the `Math.random()` failure simulation — Temporal will retry it automatically

***

### Part B – Implement the Workflow

In `FulfillmentWorkflowImpl.kt`:

1. Create `ActivityOptions` with a `StartToCloseTimeout` of **30 seconds**
2. Create a `FulfillmentActivities` stub via `Workflow.newActivityStub(...)`
3. Replace the `null` stub for `activities`
4. In `processOrder()`, call the three activities in sequence and return an `OrderResult`

***

### Part C – Run it

Once your code compiles, start both processes:

1. Click the [button label="Terminal 1 - Worker" background="#444CE7"](tab-1) tab and start the Worker:

   ```bash,run
   gradle runWorker
   ```

2. Click the [button label="Terminal 2 - Starter" background="#444CE7"](tab-2) tab and run the Starter:

   ```bash,run
   gradle runStarter
   ```

Open the [button label="Temporal Web UI" background="#444CE7"](tab-3) tab and find workflow `fulfillment-ORD-1001`.
Try killing the Worker mid-execution (Ctrl+C in Terminal 1) and restarting it — what happens?

***

Click **Check** when you think you're done. The checker will verify your source code and confirm the project compiles.

> **Stuck?** Click **Solve** to apply the reference solution and see a working implementation.
