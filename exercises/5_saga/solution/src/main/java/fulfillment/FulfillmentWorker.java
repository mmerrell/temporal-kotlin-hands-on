package fulfillment;

import io.temporal.client.WorkflowClient;
import io.temporal.client.WorkflowOptions;
import io.temporal.serviceclient.WorkflowServiceStubs;
import io.temporal.worker.Worker;
import io.temporal.worker.WorkerFactory;

public class FulfillmentWorker {
    public static void main(String[] args) {
        WorkflowServiceStubs service = WorkflowServiceStubs.newLocalServiceStubs();
        WorkflowClient client = WorkflowClient.newInstance(service);
        WorkerFactory factory = WorkerFactory.newInstance(client);

        Worker worker = factory.newWorker(Constants.TASK_QUEUE_NAME);
        worker.registerWorkflowImplementationTypes(FulfillmentWorkflowImpl.class);
        worker.registerActivitiesImplementations(new FulfillmentActivitiesImpl());

        factory.start();
        System.out.println("Worker started. Listening on task queue: " + Constants.TASK_QUEUE_NAME);
    }
}
