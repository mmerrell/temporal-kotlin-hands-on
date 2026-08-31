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

        // TODO Part B: Wrap the three activity calls below in a try/catch (e: Exception) block.
        //   Inside try, call:
        //     1. reservationId = activities.reserveInventory(order)
        //     2. paymentConfirmation = activities.processPayment(order)
        //     3. val trackingNumber = activities.dispatchToFulfillment(order, reservationId)
        //   After all three succeed, return:
        //     OrderResult(order.orderId, "FULFILLED", reservationId, paymentConfirmation, trackingNumber)
        //
        //   Leave the catch block empty for now — you'll fill it in Part C.

        // TODO Part C: In the catch block, run compensating activities in reverse order.
        //   Only compensate for steps that completed — guard each call on null:
        //     if (paymentConfirmation != null) activities.refundPayment(paymentConfirmation)
        //     if (reservationId != null) activities.releaseInventory(reservationId)
        //   Then return:
        //     OrderResult(order.orderId, "FAILED", reservationId, paymentConfirmation, null)

        // Placeholder — remove once Part B is implemented
        return OrderResult(order.orderId, "NOT_IMPLEMENTED", null, null, null)
    }
}
