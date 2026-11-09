package com.orbitalix.exception;

/** Checked root for errors that cross the simulation boundary. */
public class OrbitalIXException extends Exception {
    public OrbitalIXException(String message) { super(message); }
    public OrbitalIXException(String message, Throwable cause) { super(message, cause); }
}
