package ru.ntdev.srhr.common.audit.sender.userinfo;

public enum UserinfoRequestType {

    USERINFO("userinfo"),
    USERINFO_TEAM("userinfo_team"),
    USERINFO_USERLIST("userinfo_userlist"),
    USERINFO_CHILD("userinfo_child"),
    USERINFO_ROLES("userinfo_roles");

    private final String value;

    UserinfoRequestType(String value) {
        this.value = value;
    }

    public String value() {
        return value;
    }
}
