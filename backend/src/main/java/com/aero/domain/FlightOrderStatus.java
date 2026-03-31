package com.aero.domain;

public enum FlightOrderStatus {
    INTRODUCED(1),
    SUBMITTED(2),
    REJECTED(3),
    ACCEPTED(4),
    PARTIALLY_DONE(5),
    DONE(6),
    NOT_DONE(7);

    private final int code;

    FlightOrderStatus(int code) {
        this.code = code;
    }

    public int getCode() {
        return code;
    }
}
