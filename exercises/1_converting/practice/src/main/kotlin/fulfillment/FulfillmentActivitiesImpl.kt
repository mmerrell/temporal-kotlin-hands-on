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
        // TODO Part A1 — call the warehouse, and do nothing about failure.
        //
        //   log.info("Reserving {} x {} for order {}", order.quantity, order.itemSku, order.orderId)
        //   return WarehouseClient.reserve(order.itemSku, order.quantity, order.orderId)
        //
        // WarehouseClient throws when the service times out (see WarehouseClient.kt).
        // Let that exception propagate — don't catch it, don't wrap it.
        //
        // Temporal retries a failed Activity by default. You are not adding retry
        // behaviour here; you are inheriting it, which is the correct answer for a
        // transient infrastructure failure.
        TODO("Part A1: implement reserveInventory")
    }

    override fun processPayment(order: Order): String {
        // TODO Part A2 — decline orders over CREDIT_LIMIT.
        //
        //   log.info("Charging {} to customer {}", order.totalAmount, order.customerId)
        //
        //   if (order.totalAmount > CREDIT_LIMIT) {
        //       throw ApplicationFailure.newFailure(
        //           "Card declined: order total ${order.totalAmount} exceeds credit limit $CREDIT_LIMIT",
        //           "PaymentDeclined")
        //   }
        //
        //   return "PAY-${order.orderId}"
        //
        // Write it exactly like that for now, with newFailure. It is the wrong
        // choice and Part D is where you find out why — don't fix it yet.
        TODO("Part A2: implement processPayment")
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
