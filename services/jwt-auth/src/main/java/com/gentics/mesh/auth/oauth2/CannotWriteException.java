package com.gentics.mesh.auth.oauth2;

public class CannotWriteException extends Exception {
	private final static CannotWriteException instance = new CannotWriteException();
	private CannotWriteException() {
		super("Cannot write to database", null, false, false);
	}

	public static void throwException() throws CannotWriteException {
		throw instance;
	}

	@Override
	public synchronized Throwable fillInStackTrace() {
		return this;
	}
}
