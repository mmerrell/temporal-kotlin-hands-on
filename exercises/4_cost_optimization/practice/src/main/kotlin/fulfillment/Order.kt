package fulfillment

// Defaults + @JvmOverloads give this a real no-arg constructor, which Temporal's default
// Jackson-based DataConverter needs to deserialize activity/workflow arguments via field access.
data class Order @JvmOverloads constructor(
    val orderId: String = "",
    val customerId: String = "",
    val itemSku: String = "",
    val quantity: Int = 0,
    val totalAmount: Double = 0.0
)
