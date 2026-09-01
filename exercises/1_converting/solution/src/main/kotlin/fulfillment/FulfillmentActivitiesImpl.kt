package fulfillment

import io.temporal.failure.ApplicationFailure
import org.slf4j.LoggerFactory

class FulfillmentActivitiesImpl : FulfillmentActivities {

    companion object {
        private val log = LoggerFactory.getLogger(FulfillmentActivitiesImpl::class.java)

        // Orders above this amount exceed the customer's credit limit.
        private const val CREDIT_LIMIT = 500.0
    }

    override fun reserveInventory(order: Order): String {
        log.info("Reserving {} x {} for order {}", order.quantity, order.itemSku, order.orderId)

        // The warehouse client throws when the service times out. We let that
        // propagate untouched: Temporal retries failed Activities by default, and
        // for a transient infrastructure failure that is exactly the right answer.
        return WarehouseClient.reserve(order.itemSku, order.quantity, order.orderId)
    }

    override fun processPayment(order: Order): String {
        log.info("Charging {} to customer {}", order.totalAmount, order.customerId)

        if (order.totalAmount > CREDIT_LIMIT) {
            // A declined card is a business outcome, not a glitch. The default here
            // would be to retry, and retrying would decline four more times over
            // half a minute, so this is the one place we opt out.
            throw ApplicationFailure.newNonRetryableFailure(
                "Card declined: order total ${order.totalAmount} exceeds credit limit $CREDIT_LIMIT",
                "PaymentDeclined")
        }

        return "PAY-${order.orderId}"
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Worked example: already written. Read it before you write the two above —
    // it shows the shape an Activity takes.
    // ─────────────────────────────────────────────────────────────────────────
    override fun dispatchToFulfillment(order: Order, reservationId: String): String {
        log.info("Dispatching order {} against reservation {}", order.orderId, reservationId)
        return "TRK-${order.orderId}-${reservationId.takeLast(4)}"
    }
}
