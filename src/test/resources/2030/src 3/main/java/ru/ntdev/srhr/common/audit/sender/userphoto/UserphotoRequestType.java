package ru.ntdev.srhr.common.audit.sender.userphoto;

public enum UserphotoRequestType {

    USERPHOTO_AVATAR("userphoto_avatar");

    private final String value;

    UserphotoRequestType(String value) {
        this.value = value;
    }

    public String value() {
        return value;
    }
}
