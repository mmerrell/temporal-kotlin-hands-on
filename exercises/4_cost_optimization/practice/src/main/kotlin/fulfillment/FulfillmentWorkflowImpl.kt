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

    // TODO Part A: Create a LocalFulfillmentActivities stub using Workflow.newLocalActivityStub().
    //   Use LocalActivityOptions (not ActivityOptions) with a StartToCloseTimeout of 5 seconds.
    //   Local activities run in-process — no round-trip to the Temporal Server for scheduling.
    //   Replace TODO(...) below. Edit that one line — don't add a second
    //   `localActivities` declaration.
    private val localActivities: LocalFulfillmentActivities = TODO("Part A: create the Local Activity stub")

    override fun processOrder(order: Order): OrderResult {
        log.info("Processing order {}", order.orderId)

        // TODO Part B: Call localActivities.validateOrder(order) and
        //   localActivities.fraudCheck(order) BEFORE the child workflow invocation.
        //   These are fast in-process checks — no reason to pay for a Server round-trip.

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
