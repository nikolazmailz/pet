package ru.ntdev.srhr.common.audit.sender.paystub;

public enum PaystubRequestType {

    PAYSTUB_PERIOD("paystub_period"),
    PAYSTUB_MAIL("paystub_mail"),
    PAYSTUB_PDF("paystub_pdf"),
    PAYSTUB_BODY("paystub_body"),
    PAYSTUB_HEAD("paystub_head");

    private final String value;

    PaystubRequestType(String value) {
        this.value = value;
    }

    public String value() {
        return value;
    }
}
