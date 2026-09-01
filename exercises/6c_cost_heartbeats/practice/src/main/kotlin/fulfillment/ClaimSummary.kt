package fulfillment

// The small slice of a claim that the orchestration actually needs, plus a
// reference to where the bulky evidence lives.
//
// Defaults + @JvmOverloads give this a real no-arg constructor, which Temporal's default
// Jackson-based DataConverter needs to deserialize activity/workflow arguments via field access.
data class ClaimSummary @JvmOverloads constructor(
    val claimId: String = "",
    val orderId: String = "",
    val customerName: String = "",
    val itemSku: String = "",
    val evidenceRef: String = ""
)
