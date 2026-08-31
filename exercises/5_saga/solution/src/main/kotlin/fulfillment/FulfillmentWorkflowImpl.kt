package fulfillment

import io.temporal.activity.ActivityOptions
import io.temporal.workflow.Workflow
import java.time.Duration

class FulfillmentWorkflowImpl : FulfillmentWorkflow {

    companion object {
        private val log = Workflow.getLogger(FulfillmentWorkflowImpl::class.java)
    }

    private val activities: FulfillmentActivities = Workflow.newActivityStub(
        FulfillmentActivities::class.java,
        ActivityOptions.newBuilder()
            .setStartToCloseTimeout(Duration.ofSeconds(30))
            .build()
    )

    override fun processOrder(order: Order): OrderResult {
        log.info("Processing order {}", order.orderId)

        // Declare these before the try block so the catch block can see them.
        // They track which steps completed — compensation only runs for those.
        var reservationId: String? = null
        var paymentConfirmation: String? = null

        try {
            reservationId = activities.reserveInventory(order)
            log.info("Inventory reserved: {}", reservationId)

            paymentConfirmation = activities.processPayment(order)
            log.info("Payment confirmed: {}", paymentConfirmation)

            val trackingNumber = activities.dispatchToFulfillment(order, reservationId)
            log.info("Order dispatched: {}", trackingNumber)

            return OrderResult(order.orderId, "FULFILLED", reservationId, paymentConfirmation, trackingNumber)
        } catch (e: Exception) {
            log.warn("Order {} failed — running compensations. Cause: {}", order.orderId, e.message)

            // Compensate in reverse order — only for steps that completed
            if (paymentConfirmation != null) {
                activities.refundPayment(paymentConfirmation)
                log.info("Payment refunded: {}", paymentConfirmation)
            }
            if (reservationId != null) {
                activities.releaseInventory(reservationId)
                log.info("Inventory released: {}", reservationId)
            }

            return OrderResult(order.orderId, "FAILED", reservationId, paymentConfirmation, null)
        }
    }
}
