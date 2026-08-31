package fulfillment

import io.temporal.client.WorkflowClient
import io.temporal.serviceclient.WorkflowServiceStubs
import io.temporal.worker.WorkerFactory

fun main() {
    val service = WorkflowServiceStubs.newLocalServiceStubs()
    val client = WorkflowClient.newInstance(service)
    val factory = WorkerFactory.newInstance(client)

    val worker = factory.newWorker(Constants.TASK_QUEUE_NAME)
    worker.registerWorkflowImplementationTypes(
        FulfillmentWorkflowImpl::class.java,
        InventoryReservationWorkflowImpl::class.java
    )
    worker.registerActivitiesImplementations(
        FulfillmentActivitiesImpl(),
        LocalFulfillmentActivitiesImpl(),
        WarehouseActivitiesImpl()
    )

    factory.start()
    println("Worker started on task queue: ${Constants.TASK_QUEUE_NAME}")
}
