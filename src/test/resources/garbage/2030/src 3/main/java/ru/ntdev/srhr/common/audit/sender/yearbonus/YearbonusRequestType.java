package ru.ntdev.srhr.common.audit.sender.yearbonus;

public enum YearbonusRequestType {

    YEARBONUS_INFO("yearbonus_info"),
    YEARBONUS_DETAILS("yearbonus_details"),
    YEARBONUS_REPORT("yearbonus_report");

    private final String value;

    YearbonusRequestType(String value) {
        this.value = value;
    }

    public String value() {
        return value;
    }
}
