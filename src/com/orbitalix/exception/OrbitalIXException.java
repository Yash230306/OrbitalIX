package com.orbitalix.exception;

/**
 * Checked base exception for every fault OrbitalIX handles on purpose.
 * Demonstrates: inheritance (parent of the custom exception hierarchy), encapsulation.
 */
public class OrbitalIXException extends Exception {

    public OrbitalIXException(String message) {
        super(message);
    }

    public OrbitalIXException(String message, Throwable cause) {
        super(message, cause);
    }
}
