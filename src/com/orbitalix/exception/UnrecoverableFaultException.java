package com.orbitalix.exception;

/**
 * Thrown when several subsystems fail together and automatic recovery is not enough.
 * Demonstrates: inheritance, exception handling.
 */
public class UnrecoverableFaultException extends OrbitalIXException {

    public UnrecoverableFaultException(String message) {
        super(message);
    }

    public UnrecoverableFaultException(String message, Throwable cause) {
        super(message, cause);
    }
}
