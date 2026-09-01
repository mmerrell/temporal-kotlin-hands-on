package fulfillment

import io.temporal.activity.ActivityInterface
import io.temporal.activity.ActivityMethod

// The same claims pipeline, with two things changed:
//
//   1. Every method takes a ClaimSummary — the handful of fields orchestration
//      needs, plus a reference to the evidence document. Activities that need
//      the evidence itself read it from EvidenceStore.
//   2. validateClaim / normalizeCustomerName / extractProductMetadata are one
//      method. They are pure in-process transformations that fail and retry
//      together, so they did not need separate Activity boundaries.
//
// Activity type names are set explicitly because this interface and
// ClaimActivities share method names, and a Worker cannot register the same
// Activity type twice. Both pipelines live in the same Worker so you can run
// them side by side.
@ActivityInterface
interface LeanClaimActivities {

    // Writes the claim's evidence to the document store and returns the small
    // summary the rest of the pipeline works with.
    @ActivityMethod(name = "LeanStoreEvidence")
    fun storeEvidence(claim: DamageClaim): ClaimSummary

    @ActivityMethod(name = "LeanPrepareClaim")
    fun prepareClaim(summary: ClaimSummary): ClaimSummary

    @ActivityMethod(name = "LeanLookupWarrantyStatus")
    fun lookupWarrantyStatus(summary: ClaimSummary): String

    @ActivityMethod(name = "LeanAnalyzeDamageEvidence")
    fun analyzeDamageEvidence(summary: ClaimSummary): String

    @ActivityMethod(name = "LeanCalculateRepairEstimate")
    fun calculateRepairEstimate(summary: ClaimSummary, warrantyStatus: String): Double

    @ActivityMethod(name = "LeanReserveServiceSlot")
    fun reserveServiceSlot(summary: ClaimSummary): String

    @ActivityMethod(name = "LeanSendClaimConfirmation")
    fun sendClaimConfirmation(summary: ClaimSummary, estimate: Double, serviceSlot: String): String
}
