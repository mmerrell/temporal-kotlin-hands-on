package fulfillment;

public class OrderResult {
    private String orderId;
    private String status;
    private String reservationId;
    private String paymentConfirmation;
    private String trackingNumber;

    public OrderResult() {}

    public OrderResult(String orderId, String status,
                       String reservationId, String paymentConfirmation,
                       String trackingNumber) {
        this.orderId             = orderId;
        this.status              = status;
        this.reservationId       = reservationId;
        this.paymentConfirmation = paymentConfirmation;
        this.trackingNumber      = trackingNumber;
    }

    public String getOrderId()             { return orderId; }
    public String getStatus()              { return status; }
    public String getReservationId()       { return reservationId; }
    public String getPaymentConfirmation() { return paymentConfirmation; }
    public String getTrackingNumber()      { return trackingNumber; }

    @Override public String toString() {
        return String.format(
            "OrderResult{orderId=%s, status=%s, reservation=%s, payment=%s, tracking=%s}",
            orderId, status, reservationId, paymentConfirmation, trackingNumber);
    }
}
