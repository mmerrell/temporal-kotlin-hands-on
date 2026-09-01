package fulfillment

import io.temporal.workflow.SignalMethod
import io.temporal.workflow.WorkflowInterface
import io.temporal.workflow.WorkflowMethod

@WorkflowInterface
interface DamageClaimWorkflow {

    @WorkflowMethod
    fun processClaim(claim: DamageClaim): ClaimResult

    // The customer disputes the outcome. Business rule: a claim stays appealable
    // for 90 days after the confirmation goes out.
    @SignalMethod
    fun submitAppeal()
}
