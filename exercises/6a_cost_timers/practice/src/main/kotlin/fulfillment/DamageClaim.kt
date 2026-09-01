package fulfillment

// A customer's damage claim against a delivered order.
//
// inspectionReport and photoBundle are the adjuster's write-up and the customer's
// uploaded evidence. Together they are the bulk of the claim — a few hundred KB.
//
// Defaults + @JvmOverloads give this a real no-arg constructor, which Temporal's default
// Jackson-based DataConverter needs to deserialize activity/workflow arguments via field access.
data class DamageClaim @JvmOverloads constructor(
    val claimId: String = "",
    val orderId: String = "",
    val customerId: String = "",
    val customerName: String = "",
    val customerEmail: String = "",
    val itemSku: String = "",
    val description: String = "",
    val inspectionReport: String = "",
    val photoBundle: String = ""
)
