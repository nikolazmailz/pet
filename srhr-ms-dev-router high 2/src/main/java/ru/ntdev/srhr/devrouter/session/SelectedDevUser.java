package ru.ntdev.srhr.devrouter.session;

public record SelectedDevUser(
        String user,
        String sessionId,
        SelectionSource selectedBy
) {
    public enum SelectionSource {
        HEADER,
        COOKIE,
        DEFAULT
    }
}
