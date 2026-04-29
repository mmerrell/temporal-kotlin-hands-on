package fulfillment;

public class Order {
    private String orderId;
    private String customerId;
    private String itemSku;
    private int    quantity;
    private double totalAmount;

    public Order() {}

    public Order(String orderId, String customerId, String itemSku, int quantity, double totalAmount) {
        this.orderId     = orderId;
        this.customerId  = customerId;
        this.itemSku     = itemSku;
        this.quantity    = quantity;
        this.totalAmount = totalAmount;
    }

    public String getOrderId()     { return orderId; }
    public String getCustomerId()  { return customerId; }
    public String getItemSku()     { return itemSku; }
    public int    getQuantity()    { return quantity; }
    public double getTotalAmount() { return totalAmount; }

    @Override public String toString() {
        return String.format("Order{id=%s, sku=%s, qty=%d, amount=%.2f}",
            orderId, itemSku, quantity, totalAmount);
    }
}
