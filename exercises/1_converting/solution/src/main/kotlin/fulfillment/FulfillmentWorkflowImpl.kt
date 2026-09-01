package fulfillment

import io.temporal.activity.ActivityOptions
import io.temporal.common.RetryOptions
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
            // The retry loop from FulfillmentPipeline, expressed as configuration.
            // MAX_RETRIES = 5 and RETRY_DELAY_MS = 2000 became these two lines, and
            // Temporal does the waiting — durably, without holding a thread.
            .setRetryOptions(
                RetryOptions.newBuilder()
                    .setInitialInterval(Duration.ofSeconds(2))
                    .setMaximumAttempts(5)
                    .build()
            )
            .build()
    )

    override fun processOrder(order: Order): OrderResult {
        log.info("Processing order {}", order.orderId)

        val reservationId = activities.reserveInventory(order)
        val paymentConfirmation = activities.processPayment(order)
        val trackingNumber = activities.dispatchToFulfillment(order, reservationId)

        return OrderResult(order.orderId, "FULFILLED", reservationId, paymentConfirmation, trackingNumber)
    }
}
