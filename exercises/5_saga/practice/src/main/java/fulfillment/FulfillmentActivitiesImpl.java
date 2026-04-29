package fulfillment;

import io.temporal.failure.ApplicationFailure;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class FulfillmentActivitiesImpl implements FulfillmentActivities {

    private static final Logger log = LoggerFactory.getLogger(FulfillmentActivitiesImpl.class);

    @Override
    public String reserveInventory(Order order) {
        log.info("Reserving {} units of {} for order {}", order.getQuantity(), order.getItemSku(), order.getOrderId());
        if (Math.random() < 0.1)
            throw ApplicationFailure.newFailure("Inventory service unavailable", "InventoryError");
        return "RES-" + order.getOrderId() + "-" + System.currentTimeMillis();
    }

    @Override
    public String processPayment(Order order) {
        log.info("Processing payment ${} for order {}", order.getTotalAmount(), order.getOrderId());
        if (Math.random() < 0.2)
            throw ApplicationFailure.newFailure("Payment gateway unavailable", "PaymentError");
        return "PAY-" + order.getOrderId() + "-" + System.currentTimeMillis();
    }

    @Override
    public String dispatchToFulfillment(Order order, String reservationId) {
        log.info("Dispatching order {} with reservation {}", order.getOrderId(), reservationId);
        if (Math.random() < 0.3)
            throw ApplicationFailure.newFailure("Fulfillment API error", "DispatchError");
        return "TRK-" + reservationId.hashCode() + "-" + System.currentTimeMillis();
    }

    @Override
    public void releaseInventory(String reservationId) {
        log.info("Releasing inventory reservation {}", reservationId);
        // In production this would call the inventory service to free the hold
    }

    @Override
    public void refundPayment(String paymentConfirmation) {
        log.info("Refunding payment {}", paymentConfirmation);
        // In production this would call the payment gateway's refund endpoint
    }
}
