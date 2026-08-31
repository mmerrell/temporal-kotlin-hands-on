package fulfillment

import io.temporal.client.WorkflowClient
import io.temporal.client.WorkflowOptions
import io.temporal.serviceclient.WorkflowServiceStubs
import kotlin.system.exitProcess

fun main() {
    val service = WorkflowServiceStubs.newLocalServiceStubs()
    val client = WorkflowClient.newInstance(service)

    val order = Order("ORD-1003", "CUST-42", "SKU-ROCKET-9", 2, 299.99)

    val options = WorkflowOptions.newBuilder()
        .setWorkflowId("fulfillment-${order.orderId}")
        .setTaskQueue(Constants.TASK_QUEUE_NAME)
        .build()

    val workflow = client.newWorkflowStub(FulfillmentWorkflow::class.java, options)
    val result = workflow.processOrder(order)

    println("Result: $result")
    exitProcess(0)
}
