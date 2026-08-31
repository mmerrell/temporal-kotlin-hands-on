package fulfillment

import io.temporal.failure.ApplicationFailure
import org.slf4j.LoggerFactory

class FulfillmentActivitiesImpl : FulfillmentActivities {

    companion object {
        private val log = LoggerFactory.getLogger(FulfillmentActivitiesImpl::class.java)
    }

    override fun reserveInventory(order: Order): String {
        log.info("Reserving inventory for SKU {} qty {}", order.itemSku, order.quantity)
        if (Math.random() < 0.3)
            throw ApplicationFailure.newFailure("Inventory service timeout", "InventoryError")
        return "RES-${order.itemSku}-${System.currentTimeMillis()}"
    }

    override fun processPayment(order: Order): String {
        log.info("Processing payment \${} for {}", order.totalAmount, order.orderId)
        if (Math.random() < 0.2)
            throw ApplicationFailure.newFailure("Payment gateway unavailable", "PaymentError")
        return "PAY-${order.orderId}-${System.currentTimeMillis()}"
    }

    override fun dispatchToFulfillment(order: Order, reservationId: String): String {
        log.info("Dispatching order {} reservation {}", order.orderId, reservationId)
        if (Math.random() < 0.2)
            throw ApplicationFailure.newFailure("Fulfillment API error", "DispatchError")
        return "TRK-${reservationId.hashCode()}-${System.currentTimeMillis()}"
    }
}
