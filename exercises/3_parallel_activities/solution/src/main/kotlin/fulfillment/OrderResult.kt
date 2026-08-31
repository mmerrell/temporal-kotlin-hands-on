package fulfillment

// Defaults + @JvmOverloads give this a real no-arg constructor, which Temporal's default
// Jackson-based DataConverter needs to deserialize activity/workflow arguments via field access.
data class OrderResult @JvmOverloads constructor(
    val orderId: String = "",
    val status: String = "",
    val reservationId: String? = null,
    val paymentConfirmation: String? = null,
    val trackingNumber: String? = null
)
