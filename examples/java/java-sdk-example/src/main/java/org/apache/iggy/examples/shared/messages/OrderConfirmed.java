package org.apache.iggy.examples.shared.messages;

public class OrderConfirmed {
    private String orderId;
    private String confirmedBy;
    private String timestamp;

    // Default constructor for Jackson
    public OrderConfirmed() {}

    public OrderConfirmed(String orderId, String confirmedBy, String timestamp) {
        this.orderId = orderId;
        this.confirmedBy = confirmedBy;
        this.timestamp = timestamp;
    }

    // Getters and setters
    public String getOrderId() { return orderId; }
    public void setOrderId(String orderId) { this.orderId = orderId; }

    public String getConfirmedBy() { return confirmedBy; }
    public void setConfirmedBy(String confirmedBy) { this.confirmedBy = confirmedBy; }

    public String getTimestamp() { return timestamp; }
    public void setTimestamp(String timestamp) { this.timestamp = timestamp; }

    @Override
    public String toString() {
        return "OrderConfirmed{orderId='" + orderId + "', confirmedBy='" +
                confirmedBy + "', timestamp='" + timestamp + "'}";
    }
}
