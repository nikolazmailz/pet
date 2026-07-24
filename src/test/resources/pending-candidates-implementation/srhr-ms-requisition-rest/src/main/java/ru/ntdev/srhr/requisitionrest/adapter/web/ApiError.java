package ru.ntdev.srhr.requisitionrest.adapter.web;

public record ApiError(String code, String message, String traceId) {}
