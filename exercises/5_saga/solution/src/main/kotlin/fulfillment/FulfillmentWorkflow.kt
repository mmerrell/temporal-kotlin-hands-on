package fulfillment

import io.temporal.workflow.WorkflowInterface
import io.temporal.workflow.WorkflowMethod

@WorkflowInterface
interface FulfillmentWorkflow {

    @WorkflowMethod
    fun processOrder(order: Order): OrderResult
}
