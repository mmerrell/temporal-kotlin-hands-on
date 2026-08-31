package fulfillment

import io.temporal.activity.ActivityOptions
import io.temporal.workflow.Workflow
import java.time.Duration

class FulfillmentWorkflowImpl : FulfillmentWorkflow {

    companion object {
        private val log = Workflow.getLogger(FulfillmentWorkflowImpl::class.java)
    }

    // TODO Part B: Create ActivityOptions with a StartToCloseTimeout of 30 seconds,
    //              then create a FulfillmentActivities stub via Workflow.newActivityStub(...).
    private val activities: FulfillmentActivities? = null // replace this

    override fun processOrder(order: Order): OrderResult {
        log.info("Processing order {}", order.orderId)

        // TODO Part B: Call each activity in sequence:
        //   1. activities.reserveInventory(order)                     -> reservationId
        //   2. activities.processPayment(order)                       -> paymentConfirmation
        //   3. activities.dispatchToFulfillment(order, reservationId) -> trackingNumber
        //   Return an OrderResult with status "FULFILLED".

        TODO("Part B: implement processOrder")
    }
}
