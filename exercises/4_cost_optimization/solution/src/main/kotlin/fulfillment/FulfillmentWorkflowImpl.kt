package fulfillment

import io.temporal.activity.ActivityOptions
import io.temporal.activity.LocalActivityOptions
import io.temporal.workflow.ChildWorkflowOptions
import io.temporal.workflow.Workflow
import java.time.Duration

class FulfillmentWorkflowImpl : FulfillmentWorkflow {

    companion object {
        private val log = Workflow.getLogger(FulfillmentWorkflowImpl::class.java)
    }

    // Remote activities — routed through the task queue
    private val activities: FulfillmentActivities = Workflow.newActivityStub(
        FulfillmentActivities::class.java,
        ActivityOptions.newBuilder()
            .setStartToCloseTimeout(Duration.ofSeconds(30))
            .build()
    )

    // Local activities — run in-process, no task queue round-trip
    private val localActivities: LocalFulfillmentActivities = Workflow.newLocalActivityStub(
        LocalFulfillmentActivities::class.java,
        LocalActivityOptions.newBuilder()
            .setStartToCloseTimeout(Duration.ofSeconds(5))
            .build()
    )

    override fun processOrder(order: Order): OrderResult {
        log.info("Processing order {}", order.orderId)

        // Fast local steps — no Temporal Server round-trip
        localActivities.validateOrder(order)
        localActivities.fraudCheck(order)

        val inventoryWorkflow = Workflow.newChildWorkflowStub(
            InventoryReservationWorkflow::class.java,
            ChildWorkflowOptions.newBuilder()
                .setWorkflowId("inventory-${order.orderId}")
                .build()
        )
        val reservationId = inventoryWorkflow.reserve(order.itemSku, order.quantity)

        val paymentConfirmation = activities.processPayment(order)
        val trackingNumber = activities.dispatchToFulfillment(order, reservationId)

        return OrderResult(order.orderId, "FULFILLED", reservationId, paymentConfirmation, trackingNumber)
    }
}
