package fulfillment

import io.temporal.activity.Activity
import org.slf4j.LoggerFactory

// The evidence analysis — the one genuinely long-running step in the pipeline.
// It walks the photo bundle region by region, and reports progress back to the
// server so that a Worker crash is noticed while the Activity is still running.
object EvidenceAnalyzer {

    private val log = LoggerFactory.getLogger(EvidenceAnalyzer::class.java)

    fun analyze(claim: DamageClaim): String {
        val context = Activity.getExecutionContext()
        log.info("Analyzing {} KB of evidence for claim {}",
            claim.photoBundle.length / 1024, claim.claimId)

        var affectedRegions = 0
        for (step in 1..Config.ANALYSIS_STEPS) {
            affectedRegions += inspectRegion(claim.photoBundle, step)

            // TODO Part C: report progress at meaningful checkpoints instead of
            //   on every single step. Wrap this call in:
            //       if (step % Config.HEARTBEAT_EVERY_N_STEPS == 0) { ... }
            //   then raise HEARTBEAT_EVERY_N_STEPS and HEARTBEAT_TIMEOUT_SECONDS
            //   in Config.kt. The two settings are coupled — see the assignment.
            context.heartbeat(step)

            Thread.sleep(Config.ANALYSIS_STEP_SECONDS * 1000)
        }

        log.info("Analysis complete for claim {}: {} affected regions", claim.claimId, affectedRegions)
        return "DAMAGE_CONFIRMED:$affectedRegions"
    }

    private fun inspectRegion(photoBundle: String, step: Int): Int {
        val window = photoBundle.length / Config.ANALYSIS_STEPS
        val from = (step - 1) * window
        val slice = photoBundle.substring(from, from + window)
        return if (slice.contains("abrasion")) 1 else 0
    }
}
