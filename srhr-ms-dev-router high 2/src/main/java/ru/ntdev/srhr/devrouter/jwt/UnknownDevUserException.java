package ru.ntdev.srhr.devrouter.jwt;

public class UnknownDevUserException extends RuntimeException {

    public UnknownDevUserException(String user) {
        super("Unknown DEV user profile: " + user);
    }
}
