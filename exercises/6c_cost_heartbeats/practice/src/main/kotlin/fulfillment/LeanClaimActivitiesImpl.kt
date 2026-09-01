package fulfillment

import io.temporal.failure.ApplicationFailure
import org.slf4j.LoggerFactory
import kotlin.math.abs

class LeanClaimActivitiesImpl : LeanClaimActivities {

    companion object {
        private val log = LoggerFactory.getLogger(LeanClaimActivitiesImpl::class.java)
    }

    override fun storeEvidence(claim: DamageClaim): ClaimSummary {
        val ref = EvidenceStore.put(claim)
        log.info("Stored {} KB of evidence for claim {} at {}",
            (claim.inspectionReport.length + claim.photoBundle.length) / 1024, claim.claimId, ref)
        return ClaimSummary(
            claimId = claim.claimId,
            orderId = claim.orderId,
            customerName = claim.customerName,
            itemSku = claim.itemSku,
            evidenceRef = ref
        )
    }

    override fun prepareClaim(summary: ClaimSummary): ClaimSummary {
        log.info("Preparing claim {}", summary.claimId)
        val claim = EvidenceStore.get(summary.evidenceRef)

        if (summary.claimId.isBlank())
            throw ApplicationFailure.newNonRetryableFailure("Claim id is required", "ValidationError")
        if (claim.photoBundle.isEmpty())
            throw ApplicationFailure.newNonRetryableFailure("Claim has no evidence", "ValidationError")

        val normalizedName = summary.customerName.trim().split(" ")
            .filter { it.isNotBlank() }
            .joinToString(" ") { it.lowercase().replaceFirstChar(Char::uppercase) }

        return summary.copy(customerName = normalizedName)
    }

    override fun lookupWarrantyStatus(summary: ClaimSummary): String {
        log.info("Looking up warranty for order {}", summary.orderId)
        simulateServiceCall()
        return if (summary.orderId.hashCode() % 10 == 0) "EXPIRED" else "ACTIVE"
    }

    override fun analyzeDamageEvidence(summary: ClaimSummary): String =
        EvidenceAnalyzer.analyze(EvidenceStore.get(summary.evidenceRef))

    override fun calculateRepairEstimate(summary: ClaimSummary, warrantyStatus: String): Double {
        log.info("Calculating repair estimate for claim {}", summary.claimId)
        val repairCost = 180.0 + (abs(summary.itemSku.hashCode()) % 120).toDouble()
        // Under warranty the customer pays only the service call fee.
        return if (warrantyStatus == "ACTIVE") 49.0 else repairCost
    }

    override fun reserveServiceSlot(summary: ClaimSummary): String {
        log.info("Reserving service slot for claim {}", summary.claimId)
        simulateServiceCall()
        return "SLOT-${summary.claimId}-TUE-0900"
    }

    override fun sendClaimConfirmation(summary: ClaimSummary, estimate: Double, serviceSlot: String): String {
        log.info("Emailing confirmation for claim {}", summary.claimId)
        simulateServiceCall()
        return "CONF-${summary.claimId}"
    }

    private fun simulateServiceCall() = Thread.sleep(250)
}
