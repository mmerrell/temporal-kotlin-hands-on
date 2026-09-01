package fulfillment

import io.temporal.activity.ActivityInterface
import io.temporal.activity.ActivityMethod

// The claims pipeline as it was first written: one Activity per business step,
// each one handed the whole claim so it has everything it might need.
@ActivityInterface
interface ClaimActivities {

    @ActivityMethod
    fun validateClaim(claim: DamageClaim): DamageClaim

    @ActivityMethod
    fun normalizeCustomerName(claim: DamageClaim): DamageClaim

    @ActivityMethod
    fun extractProductMetadata(claim: DamageClaim): DamageClaim

    @ActivityMethod
    fun lookupWarrantyStatus(claim: DamageClaim): String

    @ActivityMethod
    fun analyzeDamageEvidence(claim: DamageClaim): String

    @ActivityMethod
    fun calculateRepairEstimate(claim: DamageClaim, warrantyStatus: String): Double

    @ActivityMethod
    fun reserveServiceSlot(claim: DamageClaim): String

    @ActivityMethod
    fun sendClaimConfirmation(claim: DamageClaim, estimate: Double, serviceSlot: String): String
}
