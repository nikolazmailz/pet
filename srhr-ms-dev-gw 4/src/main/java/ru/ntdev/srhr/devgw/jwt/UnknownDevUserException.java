package ru.ntdev.srhr.devgw.jwt;

public class UnknownDevUserException extends IllegalArgumentException {

    public UnknownDevUserException(String user) {
        super("Неизвестный DEV-пользователь: " + user);
    }
}
