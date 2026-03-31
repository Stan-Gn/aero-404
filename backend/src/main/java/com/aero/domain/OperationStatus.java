package com.aero.domain;

public enum OperationStatus {
    INTRODUCED(1),
    REJECTED(2),
    CONFIRMED(3),
    SCHEDULED(4),
    PARTIALLY_DONE(5),
    DONE(6),
    RESIGNED(7);

    private final int code;

    OperationStatus(int code) {
        this.code = code;
    }

    public int getCode() {
        return code;
    }
}
