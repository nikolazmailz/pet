package ru.ntdev.srhr.common.audit.sender.payavg;

public enum PayavgRequestType {

    PAYAVG_EVENT("payavg_event"),
    PAYAVG_CALCULATE("payavg_calculate");

    private final String value;

    PayavgRequestType(String value) {
        this.value = value;
    }

    public String value() {
        return value;
    }
}
