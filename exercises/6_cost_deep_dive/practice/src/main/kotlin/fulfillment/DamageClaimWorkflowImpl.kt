package fulfillment

import io.temporal.activity.ActivityOptions
import io.temporal.workflow.Workflow
import java.time.Duration

class DamageClaimWorkflowImpl : DamageClaimWorkflow {

    companion object {
        private val log = Workflow.getLogger(DamageClaimWorkflowImpl::class.java)
    }

    private var appealReceived = false

    // Most steps are quick service calls or small transformations.
    private val activities: ClaimActivities = Workflow.newActivityStub(
        ClaimActivities::class.java,
        ActivityOptions.newBuilder()
            .setStartToCloseTimeout(Duration.ofSeconds(30))
            .build()
    )

    // The evidence analysis runs far longer than the others and reports progress
    // while it works, so it gets its own options: a generous start-to-close plus
    // a heartbeat timeout.
    private val analysis: ClaimActivities = Workflow.newActivityStub(
        ClaimActivities::class.java,
        ActivityOptions.newBuilder()
            .setStartToCloseTimeout(Duration.ofMinutes(10))
            .setHeartbeatTimeout(Duration.ofSeconds(Config.HEARTBEAT_TIMEOUT_SECONDS))
            .build()
    )

    // The leaner pipeline, already wired up and ready to use. Nothing calls it yet.
    private val leanActivities: LeanClaimActivities = Workflow.newActivityStub(
        LeanClaimActivities::class.java,
        ActivityOptions.newBuilder()
            .setStartToCloseTimeout(Duration.ofSeconds(30))
            .build()
    )

    private val leanAnalysis: LeanClaimActivities = Workflow.newActivityStub(
        LeanClaimActivities::class.java,
        ActivityOptions.newBuilder()
            .setStartToCloseTimeout(Duration.ofMinutes(10))
            .setHeartbeatTimeout(Duration.ofSeconds(Config.HEARTBEAT_TIMEOUT_SECONDS))
            .build()
    )

    override fun processClaim(claim: DamageClaim): ClaimResult {
        log.info("Processing claim {} for order {}", claim.claimId, claim.orderId)

        // Every step is handed the whole claim, so it has everything it might need.
        //
        // TODO Part B: run the pipeline on a summary and an evidence reference
        //   instead of the full claim. leanActivities / leanAnalysis are already
        //   wired up above. The shape you want:
        //
        //       var summary = leanActivities.storeEvidence(claim)
        //       summary = leanActivities.prepareClaim(summary)
        //       val warrantyStatus = leanActivities.lookupWarrantyStatus(summary)
        //       val findings = leanAnalysis.analyzeDamageEvidence(summary)
        //       val estimate = leanActivities.calculateRepairEstimate(summary, warrantyStatus)
        //       val serviceSlot = leanActivities.reserveServiceSlot(summary)
        //       val confirmationId = leanActivities.sendClaimConfirmation(summary, estimate, serviceSlot)
        //
        //   Then swap `claim = workingClaim` for `evidenceRef = summary.evidenceRef`
        //   in the returned ClaimResult.
        var workingClaim = activities.validateClaim(claim)
        workingClaim = activities.normalizeCustomerName(workingClaim)
        workingClaim = activities.extractProductMetadata(workingClaim)

        val warrantyStatus = activities.lookupWarrantyStatus(workingClaim)
        val findings = analysis.analyzeDamageEvidence(workingClaim)
        val estimate = activities.calculateRepairEstimate(workingClaim, warrantyStatus)
        val serviceSlot = activities.reserveServiceSlot(workingClaim)
        val confirmationId = activities.sendClaimConfirmation(workingClaim, estimate, serviceSlot)

        log.info("Claim {} decided: warranty={} findings={} — appeal window now open",
            claim.claimId, warrantyStatus, findings)

        // The claim stays appealable for 90 days. Wake up periodically to see
        // whether the customer has disputed the outcome. The Workflow stays open
        // for the whole window so the claim record remains available.
        //
        // TODO Part A: replace this loop with a single wait over the whole window:
        //
        //       Workflow.await(Duration.ofSeconds(Config.APPEAL_WINDOW_SECONDS)) { appealReceived }
        //
        //   It returns as soon as the signal arrives, or when the window expires.
        for (tick in 1..Config.APPEAL_TICKS) {
            if (appealReceived) break
            Workflow.sleep(Duration.ofSeconds(Config.APPEAL_TICK_SECONDS))
        }

        return ClaimResult(
            claimId = claim.claimId,
            status = if (appealReceived) "APPEALED" else "SETTLED",
            warrantyStatus = warrantyStatus,
            repairEstimate = estimate,
            findings = findings,
            serviceSlot = serviceSlot,
            confirmationId = confirmationId,
            appealed = appealReceived,

            // Hand the whole claim back so the caller has the complete record.
            claim = workingClaim
        )
    }

    override fun submitAppeal() {
        log.info("Appeal received for this claim")
        appealReceived = true
    }
}
