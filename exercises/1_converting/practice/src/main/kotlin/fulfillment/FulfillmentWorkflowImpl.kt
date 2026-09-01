package fulfillment

import io.temporal.activity.ActivityOptions
import io.temporal.common.RetryOptions
import io.temporal.workflow.Workflow
import java.time.Duration

class FulfillmentWorkflowImpl : FulfillmentWorkflow {

    companion object {
        private val log = Workflow.getLogger(FulfillmentWorkflowImpl::class.java)
    }

    // TODO Part B1: replace this with a real Activity stub.
    //
    //   private val activities: FulfillmentActivities = Workflow.newActivityStub(
    //       FulfillmentActivities::class.java,
    //       ActivityOptions.newBuilder()
    //           .setStartToCloseTimeout(Duration.ofSeconds(30))
    //           .setRetryOptions(
    //               RetryOptions.newBuilder()
    //                   .setInitialInterval(Duration.ofSeconds(2))
    //                   .setMaximumAttempts(5)
    //                   .build()
    //           )
    //           .build()
    //   )
    //
    // Compare those RetryOptions with MAX_RETRIES and RETRY_DELAY_MS in
    // FulfillmentPipeline.kt. Same policy — but Temporal does the waiting, and it
    // survives the Worker dying mid-retry.
    private val activities: FulfillmentActivities? = null // replace this

    override fun processOrder(order: Order): OrderResult {
        log.info("Processing order {}", order.orderId)

        // TODO Part B2: call the three activities in sequence and return the result.
        //
        //   val reservationId = activities.reserveInventory(order)
        //   val paymentConfirmation = activities.processPayment(order)
        //   val trackingNumber = activities.dispatchToFulfillment(order, reservationId)
        //
        //   return OrderResult(order.orderId, "FULFILLED",
        //       reservationId, paymentConfirmation, trackingNumber)
        //
        // Notice what isn't here: no try/catch, no retry loop, no Thread.sleep, no
        // attempt counter. That's the 60 lines FulfillmentPipeline needed.
        TODO("Part B2: implement processOrder")
    }
}
