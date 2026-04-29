package fulfillment;

import io.temporal.activity.ActivityOptions;
import io.temporal.workflow.Workflow;
import org.slf4j.Logger;

import java.time.Duration;

public class FulfillmentWorkflowImpl implements FulfillmentWorkflow {

    private static final Logger log = Workflow.getLogger(FulfillmentWorkflowImpl.class);

    private final FulfillmentActivities activities = Workflow.newActivityStub(
        FulfillmentActivities.class,
        ActivityOptions.newBuilder()
            .setStartToCloseTimeout(Duration.ofSeconds(30))
            .build()
    );

    @Override
    public OrderResult processOrder(Order order) {
        log.info("Processing order {}", order.getOrderId());

        // Declare these before the try block so the catch block can see them.
        // They track which steps completed — compensation only runs for those.
        String reservationId       = null;
        String paymentConfirmation = null;

        // TODO Part B: Wrap the three activity calls below in a try/catch(Exception e) block.
        //   Inside try, call:
        //     1. reservationId      = activities.reserveInventory(order)
        //     2. paymentConfirmation = activities.processPayment(order)
        //     3. String trackingNumber = activities.dispatchToFulfillment(order, reservationId)
        //   After all three succeed, return:
        //     new OrderResult(order.getOrderId(), "FULFILLED",
        //         reservationId, paymentConfirmation, trackingNumber)
        //
        //   Leave the catch block empty for now — you'll fill it in Part C.

        // TODO Part C: In the catch block, run compensating activities in reverse order.
        //   Only compensate for steps that completed — guard each call on null:
        //     if (paymentConfirmation != null) activities.refundPayment(paymentConfirmation)
        //     if (reservationId != null)       activities.releaseInventory(reservationId)
        //   Then return:
        //     new OrderResult(order.getOrderId(), "FAILED", reservationId, paymentConfirmation, null)

        // Placeholder — remove once Part B is implemented
        return new OrderResult(order.getOrderId(), "NOT_IMPLEMENTED", null, null, null);
    }
}
