package fulfillment

import io.temporal.failure.ApplicationFailure
import org.slf4j.LoggerFactory

class LocalFulfillmentActivitiesImpl : LocalFulfillmentActivities {

    companion object {
        private val log = LoggerFactory.getLogger(LocalFulfillmentActivitiesImpl::class.java)
    }

    override fun validateOrder(order: Order) {
        log.info("Validating order {}", order.orderId)
        if (order.quantity <= 0)
            throw ApplicationFailure.newNonRetryableFailure("Quantity must be > 0", "ValidationError")
        if (order.totalAmount <= 0)
            throw ApplicationFailure.newNonRetryableFailure("Amount must be > 0", "ValidationError")
    }

    override fun fraudCheck(order: Order): String {
        log.info("Fraud check for customer {}", order.customerId)
        if (order.totalAmount > 10_000)
            throw ApplicationFailure.newNonRetryableFailure(
                "Order flagged by fraud check", "FraudError"
            )
        return "CLEARED"
    }
}
