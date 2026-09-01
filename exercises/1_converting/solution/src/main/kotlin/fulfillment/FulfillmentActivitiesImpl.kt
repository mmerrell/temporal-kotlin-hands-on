package fulfillment

import io.temporal.activity.Activity
import io.temporal.failure.ApplicationFailure
import org.slf4j.LoggerFactory

class FulfillmentActivitiesImpl : FulfillmentActivities {

    companion object {
        private val log = LoggerFactory.getLogger(FulfillmentActivitiesImpl::class.java)

        // Orders above this amount exceed the customer's credit limit.
        private const val CREDIT_LIMIT = 500.0
    }

    override fun reserveInventory(order: Order): String {
        // Activities can see which attempt they're on. The warehouse service is
        // flaky under load, so the first couple of calls time out.
        val attempt = Activity.getExecutionContext().info.attempt
        log.info("Reserving {} x {} for order {} (attempt {})",
            order.quantity, order.itemSku, order.orderId, attempt)

        if (attempt < 3) {
            // A timeout is a transient infrastructure problem. Calling again is
            // the right response, so this failure is retryable.
            throw ApplicationFailure.newFailure(
                "Warehouse service timed out", "InventoryError")
        }

        log.info("Inventory reserved for order {}", order.orderId)
        return "RES-${order.itemSku}-${order.orderId}"
    }

    override fun processPayment(order: Order): String {
        log.info("Charging {} to customer {}", order.totalAmount, order.customerId)

        if (order.totalAmount > CREDIT_LIMIT) {
            // A declined card is a business outcome, not a glitch. Charging the
            // same card four more times will decline four more times, so this
            // failure is non-retryable: Temporal gives up immediately.
            throw ApplicationFailure.newNonRetryableFailure(
                "Card declined: order total ${order.totalAmount} exceeds credit limit $CREDIT_LIMIT",
                "PaymentDeclined")
        }

        log.info("Payment approved for order {}", order.orderId)
        return "PAY-${order.orderId}"
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Worked example: this one is already written. Read it before you write the
    // two above — it shows the shape an Activity takes.
    // ─────────────────────────────────────────────────────────────────────────
    override fun dispatchToFulfillment(order: Order, reservationId: String): String {
        log.info("Dispatching order {} against reservation {}", order.orderId, reservationId)
        return "TRK-${order.orderId}-${reservationId.takeLast(4)}"
    }
}
