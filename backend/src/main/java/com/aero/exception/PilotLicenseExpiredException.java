package com.aero.exception;

public class PilotLicenseExpiredException extends ValidationException {
    public PilotLicenseExpiredException(String message) {
        super(message);
    }
}
