package ru.ntdev.srhr.common.audit.sender.teamcalendar;

public enum TeamCalendarRequestType {

    TEAM_CALENDAR_ORG_UNIT("team_calendar_org_unit"),
    TEAM_CALENDAR_PAGE_REFERENCE("team_calendar_page_reference"),
    TEAM_CALENDAR_USER_INFO("team_calendar_user_info"),
    TEAM_CALENDAR_SETTINGS("team_calendar_settings"),
    TEAM_CALENDAR_ORG_UNIT_EMPLOYEE("team_calendar_org_unit_employee"),
    TEAM_CALENDAR_POST_EMPLOYEE_LIST("team_calendar_post_employee_list"),
    TEAM_CALENDAR_POST_REQUEST_RESOLUTION("team_calendar_post_request_resolution"),
    TEAM_CALENDAR_GET_REQUEST_RESOLUTION("team_calendar_get_request_resolution"),
    TEAM_CALENDAR_REPORT_PROCESSING_RESULT("team_calendar_report_processing_result");

    private final String value;

    TeamCalendarRequestType(String value) {
        this.value = value;
    }

    public String value() {
        return value;
    }
}
