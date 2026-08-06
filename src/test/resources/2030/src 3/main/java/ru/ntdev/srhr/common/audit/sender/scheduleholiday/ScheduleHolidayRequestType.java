package ru.ntdev.srhr.common.audit.sender.scheduleholiday;

public enum ScheduleHolidayRequestType {

    SCHEDULE_HOLIDAY_USER("schedule_holiday_user"),
    SCHEDULE_HOLIDAY_REF("schedule_holiday_ref"),
    SCHEDULE_HOLIDAY_REQUEST("schedule_holiday_request"),
    SCHEDULE_HOLIDAY_REQUEST_INFO("schedule_holiday_request_info"),
    SCHEDULE_HOLIDAY_POST_REQUEST("schedule_holiday_post_request"),
    SCHEDULE_HOLIDAY_POST_VACATION_LIMIT("schedule_holiday_post_vacation_limit");

    private final String value;

    ScheduleHolidayRequestType(String value) {
        this.value = value;
    }

    public String value() {
        return value;
    }
}
