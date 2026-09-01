package fulfillment

import io.temporal.client.WorkflowClient
import io.temporal.serviceclient.WorkflowServiceStubs
import io.temporal.worker.WorkerFactory

fun main() {
    val service = WorkflowServiceStubs.newLocalServiceStubs()
    val client = WorkflowClient.newInstance(service)
    val factory = WorkerFactory.newInstance(client)

    val worker = factory.newWorker(Constants.TASK_QUEUE_NAME)
    worker.registerWorkflowImplementationTypes(DamageClaimWorkflowImpl::class.java)
    // Both pipelines are registered so you can switch between them and compare.
    worker.registerActivitiesImplementations(ClaimActivitiesImpl(), LeanClaimActivitiesImpl())

    factory.start()
    println("Worker started. Listening on task queue: ${Constants.TASK_QUEUE_NAME}")
}
