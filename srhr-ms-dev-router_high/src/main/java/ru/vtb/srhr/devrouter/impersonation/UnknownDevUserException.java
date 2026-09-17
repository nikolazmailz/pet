package ru.vtb.srhr.devrouter.impersonation;

import java.util.Collection;

public class UnknownDevUserException extends RuntimeException {

    private final String userId;
    private final Collection<String> availableUsers;

    public UnknownDevUserException(String userId, Collection<String> availableUsers) {
        super("Unknown DEV user: " + userId);
        this.userId = userId;
        this.availableUsers = availableUsers;
    }

    public String getUserId() {
        return userId;
    }

    public Collection<String> getAvailableUsers() {
        return availableUsers;
    }
}
