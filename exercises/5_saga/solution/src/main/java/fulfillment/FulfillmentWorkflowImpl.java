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

        String reservationId       = null;
        String paymentConfirmation = null;

        try {
            reservationId = activities.reserveInventory(order);
            log.info("Inventory reserved: {}", reservationId);

            paymentConfirmation = activities.processPayment(order);
            log.info("Payment confirmed: {}", paymentConfirmation);

            String trackingNumber = activities.dispatchToFulfillment(order, reservationId);
            log.info("Order dispatched: {}", trackingNumber);

            return new OrderResult(order.getOrderId(), "FULFILLED",
                reservationId, paymentConfirmation, trackingNumber);

        } catch (Exception e) {
            log.warn("Order {} failed — running compensations. Cause: {}", order.getOrderId(), e.getMessage());

            // Compensate in reverse order — only for steps that completed
            if (paymentConfirmation != null) {
                activities.refundPayment(paymentConfirmation);
                log.info("Payment refunded: {}", paymentConfirmation);
            }
            if (reservationId != null) {
                activities.releaseInventory(reservationId);
                log.info("Inventory released: {}", reservationId);
            }

            return new OrderResult(order.getOrderId(), "FAILED", reservationId, paymentConfirmation, null);
        }
    }
}
