package ru.ntdev.srhr.devgw.session;

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
