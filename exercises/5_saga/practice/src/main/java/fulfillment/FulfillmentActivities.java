package fulfillment;

import io.temporal.activity.ActivityInterface;
import io.temporal.activity.ActivityMethod;

@ActivityInterface
public interface FulfillmentActivities {

    @ActivityMethod
    String reserveInventory(Order order);

    @ActivityMethod
    String processPayment(Order order);

    @ActivityMethod
    String dispatchToFulfillment(Order order, String reservationId);

    // Compensating transactions — run in reverse order on failure
    @ActivityMethod
    void releaseInventory(String reservationId);

    @ActivityMethod
    void refundPayment(String paymentConfirmation);
}
