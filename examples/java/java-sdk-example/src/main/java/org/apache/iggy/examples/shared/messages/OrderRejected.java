package org.apache.iggy.examples.shared.messages;

import com.fasterxml.jackson.annotation.JsonProperty;

public class OrderRejected {
    private String orderId;
    private String reason;
    private String timestamp;

    // Default constructor for Jackson
    public OrderRejected() {}

    public OrderRejected(String orderId, String reason, String timestamp) {
        this.orderId = orderId;
        this.reason = reason;
        this.timestamp = timestamp;
    }

    // Getters and setters
    public String getOrderId() { return orderId; }
    public void setOrderId(String orderId) { this.orderId = orderId; }

    public String getReason() { return reason; }
    public void setReason(String reason) { this.reason = reason; }

    public String getTimestamp() { return timestamp; }
    public void setTimestamp(String timestamp) { this.timestamp = timestamp; }

    @Override
    public String toString() {
        return "OrderRejected{orderId='" + orderId + "', reason='" +
                reason + "', timestamp='" + timestamp + "'}";
    }
}
