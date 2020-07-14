package com.gentics.mesh.auth.oauth2;

/**
 * This exception can be thrown during the sync user process.
 * Throwing this exception causes the request to be delegated to the cluster coordinator master.
 */
public class CannotWriteException extends Exception {
	public CannotWriteException() {
		super("Cannot write to database");
	}
}
