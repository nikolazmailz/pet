package ru.ntdev.srhr.common.audit.sender.vacationreq;

public enum VacationreqRequestType {

    VACATIONREQ_REQUEST("vacationreq_request"),
    VACATIONREQ_AVDAY("vacationreq_avday"),
    VACATIONREQ_QUOTA("vacationreq_quota"),
    VACATIONREQ_REQ_BY_ID("vacationreq_req_by_id"),
    VACATIONREQ_AVDAY_CHECK("vacationreq_avday_check"),
    VACATIONREQ_DAYS_QUOTA("vacationreq_days_quota"),
    VACATIONREQ_ATTACHMENT("vacationreq_attachment"),
    VACATIONREQ_POST_REQUEST("vacationreq_post_request"),
    VACATIONREQ_DELEGATE("vacationreq_delegate"),
    VACATIONREQ_TRANSFER_AVAILABLE("vacationreq_transfer_available"),
    VACATIONREQ_POST_TRANSFER("vacationreq_post_transfer"),
    VACATIONREQ_TRANSFER_HISTORY("vacationreq_transfer_history"),
    VACATIONREQ_GET_TRANSFER("vacationreq_get_transfer"),
    VACATIONREQ_TRANSFER_CANCEL("vacationreq_transfer_cancel"),
    VACATIONREQ_RECALL_AVAILABLE("vacationreq_recall_available"),
    VACATIONREQ_POST_RECALL("vacationreq_post_recall"),
    VACATIONREQ_RECALL_HISTORY("vacationreq_recall_history"),
    VACATIONREQ_GET_RECALL("vacationreq_get_recall"),
    VACATIONREQ_RECALL_CANCEL("vacationreq_recall_cancel"),
    VACATIONREQ_CANCEL_AVAILABLE("vacationreq_cancel_available"),
    VACATIONREQ_POST_CANCEL("vacationreq_post_cancel"),
    VACATIONREQ_CANCEL_HISTORY("vacationreq_cancel_history"),
    VACATIONREQ_GET_CANCEL("vacationreq_get_cancel"),
    VACATIONREQ_CANCEL_CANCEL("vacationreq_cancel_cancel"),
    VACATIONREQ_INFOTEXT_CANCEL("vacationreq_infotext_cancel"),
    VACATIONREQ_INFOTEXT_RECALL("vacationreq_infotext_recall"),
    VACATIONREQ_CANCEL_DEBT_APPLICATION("vacationreq_cancel_debt_application"),
    VACATIONREQ_RECALL_DEBT_APPLICATION("vacationreq_recall_debt_application"),
    VACATIONREQ_RECALL_DAYS_LEFT_AFTER("vacationreq_recall_days_left_after");

    private final String value;

    VacationreqRequestType(String value) {
        this.value = value;
    }

    public String value() {
        return value;
    }
}
