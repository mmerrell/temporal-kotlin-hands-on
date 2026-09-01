package fulfillment

import io.temporal.activity.ActivityOptions
import io.temporal.workflow.Workflow
import java.time.Duration

class DamageClaimWorkflowImpl : DamageClaimWorkflow {

    companion object {
        private val log = Workflow.getLogger(DamageClaimWorkflowImpl::class.java)
    }

    private var appealReceived = false

    // Most steps are quick service calls.
    private val leanActivities: LeanClaimActivities = Workflow.newActivityStub(
        LeanClaimActivities::class.java,
        ActivityOptions.newBuilder()
            .setStartToCloseTimeout(Duration.ofSeconds(30))
            .build()
    )

    // The evidence analysis runs far longer than the others and reports progress
    // while it works, so it gets its own options: a generous start-to-close plus
    // a heartbeat timeout sized to the checkpoint interval, not to the clock.
    private val leanAnalysis: LeanClaimActivities = Workflow.newActivityStub(
        LeanClaimActivities::class.java,
        ActivityOptions.newBuilder()
            .setStartToCloseTimeout(Duration.ofMinutes(10))
            .setHeartbeatTimeout(Duration.ofSeconds(Config.HEARTBEAT_TIMEOUT_SECONDS))
            .build()
    )

    override fun processClaim(claim: DamageClaim): ClaimResult {
        log.info("Processing claim {} for order {}", claim.claimId, claim.orderId)

        // Park the evidence in the claims system's document store once, on the way
        // in. From here on the Workflow carries a summary and a reference — the
        // evidence itself never appears in Workflow History again.
        var summary = leanActivities.storeEvidence(claim)

        // Validation, name normalization, and metadata extraction are pure
        // in-process transformations that fail and retry together. One Activity.
        summary = leanActivities.prepareClaim(summary)

        val warrantyStatus = leanActivities.lookupWarrantyStatus(summary)
        val findings = leanAnalysis.analyzeDamageEvidence(summary)
        val estimate = leanActivities.calculateRepairEstimate(summary, warrantyStatus)
        val serviceSlot = leanActivities.reserveServiceSlot(summary)
        val confirmationId = leanActivities.sendClaimConfirmation(summary, estimate, serviceSlot)

        log.info("Claim {} decided: warranty={} findings={} — appeal window now open",
            claim.claimId, warrantyStatus, findings)

        // The claim stays appealable for 90 days. One timer covers the whole
        // window: await returns the moment the appeal signal flips the flag, and
        // returns false if the window expires first.
        Workflow.await(Duration.ofSeconds(Config.APPEAL_WINDOW_SECONDS)) { appealReceived }

        return ClaimResult(
            claimId = claim.claimId,
            status = if (appealReceived) "APPEALED" else "SETTLED",
            warrantyStatus = warrantyStatus,
            repairEstimate = estimate,
            findings = findings,
            serviceSlot = serviceSlot,
            confirmationId = confirmationId,
            appealed = appealReceived,

            // A pointer to the claim record, not the record itself.
            evidenceRef = summary.evidenceRef
        )
    }

    override fun submitAppeal() {
        log.info("Appeal received for this claim")
        appealReceived = true
    }
}
