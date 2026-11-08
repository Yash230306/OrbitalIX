package com.orbitalix.exception;

/**
 * Thrown when a reading is not a real number (for example a dead bus returns NaN).
 * Demonstrates: inheritance, exception handling.
 */
public class InvalidTelemetryException extends OrbitalIXException {

    public InvalidTelemetryException(String message) {
        super(message);
    }

    public InvalidTelemetryException(String message, Throwable cause) {
        super(message, cause);
    }
}
