package com.orbitalix.exception;

/**
 * Thrown when an instrument can no longer produce a trustworthy reading.
 * Demonstrates: inheritance, exception handling.
 */
public class SensorFailureException extends OrbitalIXException {

    public SensorFailureException(String message) {
        super(message);
    }

    public SensorFailureException(String message, Throwable cause) {
        super(message, cause);
    }
}
