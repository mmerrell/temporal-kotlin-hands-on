package fulfillment

import io.temporal.workflow.WorkflowInterface
import io.temporal.workflow.WorkflowMethod

@WorkflowInterface
interface InventoryReservationWorkflow {
    @WorkflowMethod
    fun reserve(sku: String, quantity: Int): String
}
