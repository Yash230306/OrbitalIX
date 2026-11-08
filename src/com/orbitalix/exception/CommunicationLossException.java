package com.orbitalix.exception;

/**
 * Thrown when the ground link drops out.
 * Demonstrates: inheritance, exception handling.
 */
public class CommunicationLossException extends OrbitalIXException {

    public CommunicationLossException(String message) {
        super(message);
    }

    public CommunicationLossException(String message, Throwable cause) {
        super(message, cause);
    }
}
