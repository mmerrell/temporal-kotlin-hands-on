package fulfillment;

import io.temporal.client.WorkflowClient;
import io.temporal.client.WorkflowOptions;
import io.temporal.serviceclient.WorkflowServiceStubs;

public class Starter {
    public static void main(String[] args) {
        WorkflowServiceStubs service = WorkflowServiceStubs.newLocalServiceStubs();
        WorkflowClient client = WorkflowClient.newInstance(service);

        Order order = new Order("ORD-1005", "CUST-42", "SKU-ROCKET-9", 2, 299.99);

        WorkflowOptions options = WorkflowOptions.newBuilder()
            .setWorkflowId("fulfillment-" + order.getOrderId())
            .setTaskQueue(Constants.TASK_QUEUE_NAME)
            .build();

        FulfillmentWorkflow workflow = client.newWorkflowStub(FulfillmentWorkflow.class, options);
        OrderResult result = workflow.processOrder(order);

        System.out.println("Result: " + result);
        System.exit(0);
    }
}
