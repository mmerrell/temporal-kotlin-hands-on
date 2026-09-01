package fulfillment

import io.temporal.client.WorkflowClient
import io.temporal.client.WorkflowFailedException
import io.temporal.client.WorkflowOptions
import io.temporal.serviceclient.WorkflowServiceStubs
import kotlin.system.exitProcess

fun main(args: Array<String>) {
    val service = WorkflowServiceStubs.newLocalServiceStubs()
    val client = WorkflowClient.newInstance(service)

    // Two orders, differing only in amount. ORD-1002 is over the credit limit.
    val order = if (args.contains("declined"))
        Order("ORD-1002", "CUST-42", "SKU-ROCKET-9", 8, 1299.99)
    else
        Order("ORD-1001", "CUST-42", "SKU-ROCKET-9", 2, 299.99)

    println("Submitting order ${order.orderId} for ${order.totalAmount}")

    val options = WorkflowOptions.newBuilder()
        .setWorkflowId("fulfillment-${order.orderId}")
        .setTaskQueue(Constants.TASK_QUEUE_NAME)
        .build()

    val workflow = client.newWorkflowStub(FulfillmentWorkflow::class.java, options)

    try {
        val result = workflow.processOrder(order)
        println("Result: $result")
    } catch (e: WorkflowFailedException) {
        // The workflow failed. Look at the cause — a non-retryable failure stops
        // the workflow the first time it happens.
        println("Workflow failed: ${e.cause?.message ?: e.message}")
    }
    exitProcess(0)
}
