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
        // TODO Part A1
        //
        // In FulfillmentPipeline.kt, reserving inventory can fail because the
        // warehouse service times out. That's a transient infrastructure problem —
        // the same call a moment later usually works.
        //
        // Implement it here, and make the failure RETRYABLE:
        //
        //     throw ApplicationFailure.newFailure("Warehouse service timed out", "InventoryError")
        //
        // To make the retry visible, fail the first two attempts and succeed on the
        // third. An Activity can see which attempt it's on:
        //
        //     val attempt = Activity.getExecutionContext().info.attempt
        //
        // Return a reservation id, e.g. "RES-${order.itemSku}-${order.orderId}"
        TODO("Part A1: implement reserveInventory")
    }

    override fun processPayment(order: Order): String {
        // TODO Part A2
        //
        // Payment fails for a completely different reason: the card is declined
        // because the order exceeds CREDIT_LIMIT. Retrying a declined card just
        // declines it again, five times, slower.
        //
        // Implement it here, and make that failure NON-RETRYABLE:
        //
        //     throw ApplicationFailure.newNonRetryableFailure(message, "PaymentDeclined")
        //
        // Return a payment confirmation, e.g. "PAY-${order.orderId}"
        //
        // Deciding which failures are worth retrying is the real work here. Temporal
        // will do exactly what you tell it, including retrying something pointless.
        TODO("Part A2: implement processPayment")
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
