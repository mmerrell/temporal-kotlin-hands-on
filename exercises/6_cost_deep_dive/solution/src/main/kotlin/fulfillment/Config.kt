package fulfillment

// Every knob that controls how much work this Workflow generates lives here.
//
// Business durations and demo durations are deliberately kept apart: the real
// process runs for months, the demo has to fit in a coffee break.
object Config {

    // ── Evidence payload ────────────────────────────────────────────────────
    // The adjuster's write-up and the customer's uploaded photos.
    const val INSPECTION_REPORT_KB = 50
    const val PHOTO_BUNDLE_KB = 150

    // ── Evidence analysis ───────────────────────────────────────────────────
    // The analysis genuinely takes a while: it walks the photo bundle region by
    // region. Business duration: 20-40 minutes. Demo: one second per region.
    const val ANALYSIS_STEPS = 30
    const val ANALYSIS_STEP_SECONDS = 1L

    // How often the analysis reports progress back to the server, measured in
    // analysis steps. The analysis has no need for one-second checkpoint
    // granularity — reporting every tenth region is plenty.
    const val HEARTBEAT_EVERY_N_STEPS = 10

    // How long the server waits for a progress report before deciding the
    // Worker died. Must be comfortably longer than the gap between heartbeats.
    const val HEARTBEAT_TIMEOUT_SECONDS = 60L

    // ── Appeal window ───────────────────────────────────────────────────────
    // Business rule: a claim stays appealable for 90 days, and the original
    // implementation checks hourly — about 2,160 wake-ups per claim.
    // Demo: 60 ticks of one second.
    const val APPEAL_TICKS = 60
    const val APPEAL_TICK_SECONDS = 1L

    // The same window expressed as a single duration.
    const val APPEAL_WINDOW_SECONDS = APPEAL_TICKS * APPEAL_TICK_SECONDS
}
