# Temporal Kotlin Hands-On Lab

Instruqt track: six progressive coding challenges using the Temporal Java SDK from Kotlin.
Built on a durable order-fulfillment scenario (inventory reservation, payment, dispatch)
that mirrors real production patterns.

## Track Challenges

| # | Slug | Concept | Key APIs |
|---|------|---------|----------|
| 1 | `converting` | Replace retry loops with Activities + Workflow | `newActivityStub`, `ActivityOptions`, `ApplicationFailure` |
| 2 | `child-workflows` | Decompose into child workflow with separate history | `newChildWorkflowStub`, `ChildWorkflowOptions` |
| 3 | `parallel-activities` | Fan out warehouse checks concurrently | `Async.function()`, `Promise.allOf()` |
| 4 | `cost-optimization` | Move fast steps to Local Activities | `newLocalActivityStub`, `LocalActivityOptions` |
| 5 | `saga` | Compensating transactions on failure | try/catch, null-guarded compensation |

## Repo Layout

```
temporal-kotlin-hands-on/
├── docker/
│   └── Dockerfile                  # eclipse-temurin:17-jdk + Gradle + Temporal CLI
├── exercises/                      # Exercise source baked into the Docker image
│   ├── 1_converting/
│   │   ├── practice/               # Learner starting point (TODO(...) stubs)
│   │   └── solution/               # Reference implementation
│   ├── 2_child_workflows/
│   ├── 3_parallel_activities/
│   ├── 4_cost_optimization/
│   ├── 5_saga/
│   └── 6_cost_deep_dive/
├── scripts/
│   └── bootstrap-exercises.sh      # Populate exercises/ from any workshop repo
├── track/                          # Instruqt track definition
│   ├── track.yml
│   ├── 01-converting/
│   │   ├── config.yml              # Container + tab layout
│   │   ├── assignment.md           # Learner instructions (rendered in sidebar)
│   │   ├── setup-shell             # Copies practice/ to /home/user/exercise
│   │   ├── check-shell             # Source grep + gradle compileKotlin
│   │   ├── solve-shell             # Copies solution/ over practice/
│   │   └── cleanup-shell
│   ├── 02-child-workflows/
│   ├── 03-parallel-activities/
│   ├── 04-cost-optimization/
│   ├── 05-saga/
│   └── 06-cost-deep-dive/
└── .github/workflows/build-image.yml  # Rebuilds sandbox image on push to main
```

## Container Architecture

Each Instruqt challenge runs **two containers**:

| Container | Image | Role |
|-----------|-------|------|
| `sandbox` | `ghcr.io/mmerrell/temporal-kotlin-sandbox:latest` | Learner's terminal (JDK 17, Gradle, Temporal CLI, exercise code) |
| `temporal-server` | `temporalio/temporal:latest` | Local dev server — Web UI on port 8233 |

The sandbox connects to the server via `TEMPORAL_ADDRESS=temporal-server:7233`.
Learners never manage the Temporal server process — it's always running as a sidecar.

Learners get **three tabs** per challenge:
- **Terminal 1** — for running the Worker (`gradle runWorker`)
- **Terminal 2** — for running the Starter (`gradle runStarter`)
- **Temporal Web UI** — service tab pointing at `temporal-server:8233`

## Setting Up the Exercises Directory

Use `scripts/bootstrap-exercises.sh` to populate `exercises/` from any workshop repo.
The script auto-discovers exercises (any directory containing both `practice/` and
`solution/` subdirectories), or you can specify them explicitly.

```bash
chmod +x scripts/bootstrap-exercises.sh

# Auto-discover all exercises from a workshop repo
./scripts/bootstrap-exercises.sh ~/Projects/coupang-mountainview-mar-2026

# Or specify exercises explicitly
./scripts/bootstrap-exercises.sh ~/Projects/coupang-mountainview-mar-2026 1_converting 2_child_workflows

# Works with any workshop repo that follows the practice/solution layout
./scripts/bootstrap-exercises.sh ~/Projects/some-other-workshop
```

After running, commit `exercises/` and push — this triggers the image rebuild.

```bash
git add exercises/ && git commit -m 'Add exercises from coupang-mountainview-mar-2026'
git push origin main
```

**Alternative — Git subtree (keeps exercises in sync with the source repo):**
```bash
git remote add workshop git@github.com:temporal-io/coupang-mountainview-mar-2026.git
git subtree add --prefix=exercises workshop main --squash
# To pull updates later:
git subtree pull --prefix=exercises workshop main --squash
```

## Building the Image Locally

```bash
# From the repo root
docker build -f docker/Dockerfile -t temporal-kotlin-sandbox:local .

# Verify the contents
docker run -it --rm temporal-kotlin-sandbox:local bash
# Inside: java -version, gradle -version, temporal --version, ls /opt/exercises/
```

## CI/CD

On every push to `main` that touches `docker/Dockerfile` or `exercises/**`,
GitHub Actions rebuilds and pushes:
- `ghcr.io/mmerrell/temporal-kotlin-sandbox:latest`
- `ghcr.io/mmerrell/temporal-kotlin-sandbox:sha-<short-sha>` (pinned reference)

**First-time setup:**
1. Ensure the repo's GitHub Actions have `packages: write` permission
   (Settings → Actions → General → Workflow permissions → Read and write)
2. Push to `main` — the action will build and push automatically

**Subsequent exercise updates:** Edit files under `exercises/`, push to `main`.
The action rebuilds the image with the new content baked in.

## Pushing to Instruqt

```bash
cd track/
instruqt track push temporal/temporal-kotlin-hands-on
```

**First push:** The `id: ""` fields in `track.yml` and each `assignment.md` will be
populated by Instruqt. Pull the track after first push to capture the generated IDs:
```bash
instruqt track pull temporal/temporal-kotlin-hands-on
```

## Check Script Strategy

All check scripts use **source grep + `gradle compileKotlin`** — no end-to-end workflow execution.

This means checks are fast (~10s for compile) and don't flake on timing. The tradeoff
is that a learner could theoretically pass checks with syntactically correct but
logically wrong code. In practice the grep checks are surgical enough to catch the
common failure modes, and learners are encouraged to actually run the workflow to
validate their work.

**Iteration pattern:** When a check is too loose or too strict after real-learner
testing, edit `check-shell` and `instruqt track push` — no image rebuild needed
unless the Dockerfile or exercise source changed.

## Temporal CLI Version Notes

The Dockerfile pins Temporal CLI to `1.3.0`. The exercises use Temporal Java SDK
`1.31.0` (called from Kotlin — there is no separate Temporal Kotlin SDK). If you
upgrade the SDK, check the CLI release notes for any dev server compatibility
requirements before bumping `TEMPORAL_CLI_VERSION` in the Dockerfile.
