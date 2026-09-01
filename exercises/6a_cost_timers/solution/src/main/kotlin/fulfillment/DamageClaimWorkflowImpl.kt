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

    override fun processClaim(claim: DamageClaim): ClaimResult {
        log.info("Processing claim {} for order {}", claim.claimId, claim.orderId)

        // Every step is handed the whole claim, so it has everything it might need.
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

            // Hand the whole claim back so the caller has the complete record.
            claim = workingClaim
        )
    }

    override fun submitAppeal() {
        log.info("Appeal received for this claim")
        appealReceived = true
    }
}
