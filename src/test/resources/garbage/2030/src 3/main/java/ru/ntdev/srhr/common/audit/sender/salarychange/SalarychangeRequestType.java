package ru.ntdev.srhr.common.audit.sender.salarychange;

public enum SalarychangeRequestType {

    SALARYCHANGE_STAFF_DEPENDENCY("salarychange_staff_dependency"),
    SALARYCHANGE_STAFF_DEPENDENCY_EXP("salarychange_staff_dependency_exp"),
    SALARYCHANGE_STAFF_DETAIL("salarychange_staff_detail"),
    SALARYCHANGE_POST_STAFF_DETAIL("salarychange_post_staff_detail"),
    SALARYCHANGE_STAFF_DELEGATION_LIST("salarychange_staff_delegation_list"),
    SALARYCHANGE_STAFF_LIST("salarychange_staff_list"),
    SALARYCHANGE_STAFF_LIST_DETAIL("salarychange_staff_list_detail"),
    SALARYCHANGE_POST_STAFF_LIST("salarychange_post_staff_list"),
    SALARYCHANGE_POST_STAFF_LIST_EVENT("salarychange_post_staff_list_event"),
    SALARYCHANGE_REFERENCE_CITY("salarychange_reference_city"),
    SALARYCHANGE_REFERENCE_STAFF_POSITION("salarychange_reference_staff_position"),
    SALARYCHANGE_REFERENCE_TRM("salarychange_reference_trm"),
    SALARYCHANGE_REFERENCE_DELEGATION("salarychange_reference_delegation"),
    SALARYCHANGE_REFERENCE_REF("salarychange_reference_ref"),
    SALARYCHANGE_REFERENCE_TAB("salarychange_reference_tab"),
    SALARYCHANGE_POST_DELEGATION("salarychange_post_delegation"),
    SALARYCHANGE_REPORT_TASK_STATUS("salarychange_report_task_status"),
    SALARYCHANGE_POST_REPORT_TASK("salarychange_post_report_task"),
    SALARYCHANGE_REPORT_ATTACHMENT("salarychange_report_attachment");

    private final String value;

    SalarychangeRequestType(String value) {
        this.value = value;
    }

    public String value() {
        return value;
    }
}
