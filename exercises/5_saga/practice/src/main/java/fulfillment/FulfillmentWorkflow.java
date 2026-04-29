package fulfillment;

import io.temporal.workflow.WorkflowInterface;
import io.temporal.workflow.WorkflowMethod;

@WorkflowInterface
public interface FulfillmentWorkflow {

    @WorkflowMethod
    OrderResult processOrder(Order order);
}
