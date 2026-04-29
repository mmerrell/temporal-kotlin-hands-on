package fulfillment;

import io.temporal.client.WorkflowClient;
import io.temporal.client.WorkflowOptions;
import io.temporal.serviceclient.WorkflowServiceStubs;

/**
 * Sends a cancelOrder signal to a running workflow.
 * Run this from Terminal 3 while the workflow is in progress.
 *
 * Usage: mvn exec:java -Dexec.mainClass="fulfillment.CancelStarter"
 */
public class CancelStarter {
    public static void main(String[] args) {
        WorkflowServiceStubs service = WorkflowServiceStubs.newLocalServiceStubs();
        WorkflowClient client = WorkflowClient.newInstance(service);

        // Get a stub for the running workflow by its ID
        FulfillmentWorkflow workflow = client.newWorkflowStub(
            FulfillmentWorkflow.class,
            "fulfillment-ORD-1005"
        );

        System.out.println("Sending cancelOrder signal...");
        workflow.cancelOrder("Customer requested cancellation");
        System.out.println("Signal sent. Check the Web UI for compensation activity events.");
        System.exit(0);
    }
}
