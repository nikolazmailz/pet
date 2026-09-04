package ru.ntdev.srhr.common.audit.sender.timesheet;

public enum TimesheetRequestType {

    TIMESHEET_REFERENCE_EVENT("timesheet_reference_event"),
    TIMESHEET_REFERENCE_REPORT("timesheet_reference_report"),
    TIMESHEET_ORGUNIT("timesheet_orgunit"),
    TIMESHEET_GET_STAFF_LIST("timesheet_get_staff_list"),
    TIMESHEET_GET_STAFF("timesheet_get_staff"),
    TIMESHEET_POST_STAFF("timesheet_post_staff"),
    TIMESHEET_SCHEDULE("timesheet_schedule"),
    TIMESHEET_TASK_RESULT("timesheet_task_result"),
    TIMESHEET_REPORT_TASK_RESULT("timesheet_report_task_result"),
    TIMESHEET_ATTACHMENT("timesheet_attachment"),
    TIMESHEET_GET_EVENT("timesheet_get_event"),
    TIMESHEET_DELETE_EVENT("timesheet_delete_event"),
    TIMESHEET_PRESENCE_EVENT("timesheet_presence_event"),
    TIMESHEET_ABSENCE_EVENT("timesheet_absence_event"),
    TIMESHEET_SUBSTITUTION_EVENT("timesheet_substitution_event"),
    TIMESHEET_PRINT("timesheet_print"),
    TIMESHEET_LAUNCH("timesheet_launch"),
    TIMESHEET_REPORT_PARAMS("timesheet_report_params"),
    TIMESHEET_REPORT_TEMPLATE("timesheet_report_template"),
    TIMESHEET_REPORT_LOG("timesheet_report_log"),
    TIMESHEET_REPORT_PROCESSING_RESULT("timesheet_report_processing_result"),
    TIMESHEET_REPORT_PRESENCE("timesheet_report_presence"),
    TIMESHEET_REPORT_IWSU("timesheet_report_iwsu"),
    TIMESHEET_REPORT_IWSD("timesheet_report_iwsd"),
    TIMESHEET_REPORT_WRKT("timesheet_report_wrkt"),
    TIMESHEET_REPORT_FWTS("timesheet_report_fwts"),
    TIMESHEET_ATTACHMENT_TYPES("timesheet_attachment_types"),
    TIMESHEET_REPORT_APPROVER("timesheet_report_approver"),
    TIMESHEET_ASSET("timesheet_asset"),
    TIMESHEET_REQUEST("timesheet_request"),
    TIMESHEET_REQUEST_DETAILS("timesheet_request_details"),
    TIMESHEET_ATTACHMENT_CONTENT("timesheet_attachment_content"),
    TIMESHEET_ATTACHMENT_LIST("timesheet_attachment_list"),
    TIMESHEET_GET_REPORT_APPROXIMATE_TIME("timesheet_get_report_approximate_time"),
    TIMESHEET_POST_REPORT_APPROXIMATE_TIME("timesheet_post_report_approximate_time");

    private final String value;

    TimesheetRequestType(String value) {
        this.value = value;
    }

    public String value() {
        return value;
    }
}
