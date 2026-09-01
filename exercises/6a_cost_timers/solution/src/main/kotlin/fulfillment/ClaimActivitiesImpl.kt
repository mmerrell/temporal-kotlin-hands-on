package fulfillment

import io.temporal.failure.ApplicationFailure
import org.slf4j.LoggerFactory
import kotlin.math.abs

class ClaimActivitiesImpl : ClaimActivities {

    companion object {
        private val log = LoggerFactory.getLogger(ClaimActivitiesImpl::class.java)
    }

    override fun validateClaim(claim: DamageClaim): DamageClaim {
        log.info("Validating claim {}", claim.claimId)
        if (claim.claimId.isBlank())
            throw ApplicationFailure.newNonRetryableFailure("Claim id is required", "ValidationError")
        if (claim.photoBundle.isEmpty())
            throw ApplicationFailure.newNonRetryableFailure("Claim has no evidence", "ValidationError")
        return claim
    }

    override fun normalizeCustomerName(claim: DamageClaim): DamageClaim {
        log.info("Normalizing customer name for claim {}", claim.claimId)
        val normalized = claim.customerName.trim().split(" ")
            .filter { it.isNotBlank() }
            .joinToString(" ") { it.lowercase().replaceFirstChar(Char::uppercase) }
        return claim.copy(customerName = normalized)
    }

    override fun extractProductMetadata(claim: DamageClaim): DamageClaim {
        log.info("Extracting product metadata for claim {}", claim.claimId)
        val model = claim.itemSku.substringAfter("SKU-").substringBeforeLast("-")
        return claim.copy(description = "[$model] ${claim.description}")
    }

    override fun lookupWarrantyStatus(claim: DamageClaim): String {
        log.info("Looking up warranty for order {}", claim.orderId)
        simulateServiceCall()
        return if (claim.orderId.hashCode() % 10 == 0) "EXPIRED" else "ACTIVE"
    }

    override fun analyzeDamageEvidence(claim: DamageClaim): String =
        EvidenceAnalyzer.analyze(claim)

    override fun calculateRepairEstimate(claim: DamageClaim, warrantyStatus: String): Double {
        log.info("Calculating repair estimate for claim {}", claim.claimId)
        val repairCost = 180.0 + (abs(claim.itemSku.hashCode()) % 120).toDouble()
        // Under warranty the customer pays only the service call fee.
        return if (warrantyStatus == "ACTIVE") 49.0 else repairCost
    }

    override fun reserveServiceSlot(claim: DamageClaim): String {
        log.info("Reserving service slot for claim {}", claim.claimId)
        simulateServiceCall()
        return "SLOT-${claim.claimId}-TUE-0900"
    }

    override fun sendClaimConfirmation(claim: DamageClaim, estimate: Double, serviceSlot: String): String {
        log.info("Emailing confirmation to {} for claim {}", claim.customerEmail, claim.claimId)
        simulateServiceCall()
        return "CONF-${claim.claimId}"
    }

    // Stands in for a network round-trip to the warranty, scheduling, or email service.
    private fun simulateServiceCall() = Thread.sleep(250)
}
