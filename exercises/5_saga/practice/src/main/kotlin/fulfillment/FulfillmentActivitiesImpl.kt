package fulfillment

import io.temporal.failure.ApplicationFailure
import org.slf4j.LoggerFactory

class FulfillmentActivitiesImpl : FulfillmentActivities {

    companion object {
        private val log = LoggerFactory.getLogger(FulfillmentActivitiesImpl::class.java)
    }

    override fun reserveInventory(order: Order): String {
        log.info("Reserving {} units of {} for order {}", order.quantity, order.itemSku, order.orderId)
        if (Math.random() < 0.1)
            throw ApplicationFailure.newFailure("Inventory service unavailable", "InventoryError")
        return "RES-${order.orderId}-${System.currentTimeMillis()}"
    }

    override fun processPayment(order: Order): String {
        log.info("Processing payment \${} for order {}", order.totalAmount, order.orderId)
        if (Math.random() < 0.2)
            throw ApplicationFailure.newFailure("Payment gateway unavailable", "PaymentError")
        return "PAY-${order.orderId}-${System.currentTimeMillis()}"
    }

    override fun dispatchToFulfillment(order: Order, reservationId: String): String {
        log.info("Dispatching order {} with reservation {}", order.orderId, reservationId)
        if (Math.random() < 0.3)
            throw ApplicationFailure.newFailure("Fulfillment API error", "DispatchError")
        return "TRK-${reservationId.hashCode()}-${System.currentTimeMillis()}"
    }

    override fun releaseInventory(reservationId: String) {
        log.info("Releasing inventory reservation {}", reservationId)
        // In production this would call the inventory service to free the hold
    }

    override fun refundPayment(paymentConfirmation: String) {
        log.info("Refunding payment {}", paymentConfirmation)
        // In production this would call the payment gateway's refund endpoint
    }
}
