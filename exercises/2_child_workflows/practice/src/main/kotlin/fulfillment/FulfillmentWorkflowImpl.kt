package fulfillment

import io.temporal.activity.ActivityOptions
import io.temporal.workflow.ChildWorkflowOptions
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

        // TODO Part C: Create an InventoryReservationWorkflow child workflow stub.
        //   Use Workflow.newChildWorkflowStub(InventoryReservationWorkflow::class.java, ...)
        //   with ChildWorkflowOptions that set workflowId = "inventory-${order.orderId}".
        //   Call inventoryWorkflow.reserve(order.itemSku, order.quantity) to get reservationId.
        val reservationId: String = TODO("Part C: implement child workflow stub")

        val paymentConfirmation = activities.processPayment(order)
        val trackingNumber = activities.dispatchToFulfillment(order, reservationId)

        return OrderResult(order.orderId, "FULFILLED", reservationId, paymentConfirmation, trackingNumber)
    }
}
