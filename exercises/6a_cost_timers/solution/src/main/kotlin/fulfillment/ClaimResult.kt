package fulfillment

// Defaults + @JvmOverloads give this a real no-arg constructor, which Temporal's default
// Jackson-based DataConverter needs to deserialize activity/workflow arguments via field access.
data class ClaimResult @JvmOverloads constructor(
    val claimId: String = "",
    val status: String = "",
    val warrantyStatus: String? = null,
    val repairEstimate: Double = 0.0,
    val findings: String? = null,
    val serviceSlot: String? = null,
    val confirmationId: String? = null,
    val appealed: Boolean = false,

    // The full claim, echoed back so the caller has the complete record on hand.
    val claim: DamageClaim? = null,

    // A pointer to the claim record instead of the record itself.
    val evidenceRef: String? = null
)
