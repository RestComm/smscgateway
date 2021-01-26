package org.mobicents.smsc.library;

public enum DeliveryStatusType {

    DELIVERY_ACK_STATE_DELIVERED("DELIVRD"),
    DELIVERY_ACK_STATE_UNDELIVERABLE("UNDELIV"),
    DELIVERY_ACK_STATE_ENROUTE("ENROUTE");

    private String value;
    DeliveryStatusType(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }

    @Override
    public String toString() {
        return this.getValue();
    }

}
