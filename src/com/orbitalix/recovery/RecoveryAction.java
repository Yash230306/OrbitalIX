package com.orbitalix.recovery;

import com.orbitalix.exception.OrbitalIXException;
import com.orbitalix.model.Spacecraft;

/**
 * One thing the craft can do to try to save itself.
 * Demonstrates: Command pattern, abstraction.
 * Association: execute() is handed a spacecraft that this action does not own.
 */
public interface RecoveryAction {

    void execute(Spacecraft spacecraft) throws OrbitalIXException;
}
