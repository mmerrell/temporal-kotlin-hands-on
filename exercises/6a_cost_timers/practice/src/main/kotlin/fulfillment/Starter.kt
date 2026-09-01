package fulfillment

import io.temporal.client.WorkflowClient
import io.temporal.client.WorkflowOptions
import io.temporal.serviceclient.WorkflowServiceStubs
import kotlin.system.exitProcess

fun main() {
    val service = WorkflowServiceStubs.newLocalServiceStubs()
    val client = WorkflowClient.newInstance(service)

    val claim = ClaimGenerator.generate(claimId = "CLM-2001", orderId = "ORD-1006")
    val claimSizeKb = (claim.inspectionReport.length + claim.photoBundle.length) / 1024

    println("Submitting claim ${claim.claimId} carrying ~$claimSizeKb KB of evidence")
    println("This takes roughly ${estimatedRuntimeSeconds()} seconds — the analysis step and the")
    println("appeal window both have to elapse. Watch the Event History while it runs.")

    val options = WorkflowOptions.newBuilder()
        .setWorkflowId("claim-${claim.claimId}")
        .setTaskQueue(Constants.TASK_QUEUE_NAME)
        .build()

    val workflow = client.newWorkflowStub(DamageClaimWorkflow::class.java, options)
    val result = workflow.processClaim(claim)

    println("Result: status=${result.status} warranty=${result.warrantyStatus} " +
        "estimate=${result.repairEstimate} slot=${result.serviceSlot} appealed=${result.appealed}")
    exitProcess(0)
}

private fun estimatedRuntimeSeconds(): Long =
    Config.ANALYSIS_STEPS * Config.ANALYSIS_STEP_SECONDS +
        Config.APPEAL_TICKS * Config.APPEAL_TICK_SECONDS + 5
