package fulfillment;

public class OrderCancelledException extends RuntimeException {
    public OrderCancelledException(String reason) {
        super("Order cancelled: " + reason);
    }
}
