# Exercise 6 — Instructor Guide

Reveals the answers. Don't hand this to learners.

## What this exercise is for

Exercise 4 taught one cost lever (Local Activities for fast in-process steps).
This one teaches the three that usually matter more — **payload size**, **timer
count**, and **heartbeat frequency** — and deliberately undercuts the lesson
learners most often over-generalize from Exercise 4: that fewer Activities means
lower cost.

The baseline is written to be *believable*. Every choice in it is locally
reasonable. Nobody should read it and think "no one would write this."

## Measured before/after

All numbers from a real run against `temporal server start-dev`, one claim,
`PAYLOAD_SIZE` defaults (50 KB report + 150 KB photo bundle = 200 KB):

| | baseline | optimized | change |
|---|---|---|---|
| history bytes (`workflow show --output json`) | 3,757,996 | 585,738 | −84% |
| total events | 353 | 52 | −85% |
| `ACTIVITY_TASK_SCHEDULED` | 8 | 7 | −1 |
| `TIMER_STARTED` | 60 | 1 | −98% |
| heartbeat RPCs | 16 | 2 | −87% |
| wall clock | 97s | 93s | ~same |

Business outcome identical in both: `status=SETTLED warranty=ACTIVE estimate=49.0
slot=SLOT-CLM-2001-TUE-0900 appealed=false`.

## The five findings, and what to say about each

### 1. Large payload duplication

200 KB of evidence travels through eight Activity inputs and most of their results.
3.7 MB of History for one claim.

Target: the document lives in the claims system's document store; Temporal carries
a stable reference plus the small amount of state orchestration actually needs.

Talking point: the evidence still hits History **once** — in the Workflow input and
in `storeEvidence`. Once is the price of accepting the claim. Nine times was the bug.
If someone asks how to remove even that: the Starter uploads the evidence and passes
a reference in, so the Workflow never sees the document. Worth discussing; not worth
implementing in the exercise, because it makes the Workflow signature the lesson
instead of the payload.

### 2. Workflow as long-term document storage

The Workflow stays open through the appeal window partly so "the claim record stays
available." That reasoning is the actual bug, and it's the one that scales worst:
large History × many executions × long open duration × long closed retention.

Target: business and audit artifacts live in the system of record. Temporal
retention is sized from operational and debugging needs.

**This is the part the exercise cannot demonstrate**, because a dev server has no
meaningful retention configuration and no Cloud storage meter. It's prose in the
assignment and a discussion prompt here. If you have a Cloud namespace handy, this
is the moment to show a retention setting on a real namespace.

### 3. Excessive timers

60 timers in demo mode; hourly polling across a 90-day appeal window is roughly
2,160 per claim in production.

Target: `Workflow.await(Duration, Supplier<Boolean>)` — one timer for the window.

Talking point worth making explicitly: the fix is not only cheaper, it is **more
correct**. The polling loop could leave an appeal unnoticed for up to a full tick.
`await` returns the moment the signal lands. Cost and correctness point the same
way here, which is not always true and is worth flagging when it is.

### 4. Excessively granular Activities

`validateClaim`, `normalizeCustomerName`, and `extractProductMetadata` are pure
in-process transformations with no independent failure semantics.

Target: choose Activity boundaries from failure and retry semantics. Consolidate
operations that share a boundary.

**Do not let this land as "fewer Activities is better."** The measured numbers are
your ally: Activity count went 8 → 7 and contributed almost nothing to the saving.
Also worth drawing the contrast with Exercise 4 — there the answer was "make it a
Local Activity," here it's "it isn't a separate operation at all." Different
questions: *does this need a durable boundary* versus *does this need a round trip*.

### 5. Excessive heartbeat frequency

The analysis heartbeats every second for a job with no need for one-second
checkpoints.

Target: heartbeat at a frequency matched to how fast you need to detect a dead
Worker.

**The interesting finding, and the one that surprises people:** the baseline calls
`heartbeat()` 30 times but only produces ~16 RPCs. The SDK already throttles
heartbeats relative to the heartbeat *timeout*. Raising the timeout from 5s to 60s
is what takes it to ~2 — the checkpoint guard alone would not have done it. So the
heartbeat timeout is the real cost lever, and the two settings are coupled: raise
the checkpoint interval without raising the timeout and the Activity dies mid-run
with a heartbeat timeout. That failure is worth triggering live if you have time —
set `HEARTBEAT_EVERY_N_STEPS = 10` and leave `HEARTBEAT_TIMEOUT_SECONDS = 5`.

Do not let anyone conclude "remove heartbeats from long Activities."

## Running it

Each claim takes ~95 seconds by design: 30s of analysis, 60s of appeal window.
Learners should open the Event History *while it runs* rather than waiting.

To exercise the signal path mid-window:

```bash
temporal workflow signal --workflow-id claim-CLM-2001 --name submitAppeal
```

Baseline returns `status=APPEALED` up to a tick late; the optimized version
returns immediately.

## Gotchas

- **Heartbeats are not Event History events.** `grep ACTIVITY_TASK_HEARTBEAT`
  returns 0, always. Counting them needs the dev server's metrics endpoint, which
  `track_scripts/setup-workshop` exposes on `:9090` via `--metrics-port`. If that
  flag is missing, the heartbeat measurement silently returns nothing.
- **Both Activity interfaces are registered on one Worker.** `ClaimActivities` and
  `LeanClaimActivities` share five method names, so `LeanClaimActivities` sets
  explicit `@ActivityMethod(name = "Lean...")` values. Without them the Worker
  refuses to start with a duplicate-Activity-type error.
- **The Web UI gets sluggish** rendering 200 KB payloads across a 353-event
  history. That's the lesson made tactile, not a bug — but say so, or learners
  will assume the lab is broken.
- **`EvidenceStore` is an in-process map.** Not durable, not shared between
  Workers. It stands in for S3 so the exercise needs no infrastructure. Say this
  out loud; there's a comment in the file, and someone will still ask.
- **Payload size is `Config.INSPECTION_REPORT_KB` + `PHOTO_BUNDLE_KB`.** 200 KB
  keeps clear of the per-payload and history-size warning thresholds while still
  producing 3.7 MB of History. Raising it for effect is fine; verify a full run
  first, because the ceiling is a hard server limit, not a soft one.
