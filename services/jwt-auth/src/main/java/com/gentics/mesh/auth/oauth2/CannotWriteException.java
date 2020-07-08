package com.gentics.mesh.auth.oauth2;

public class CannotWriteException extends Exception {
	public final static CannotWriteException INSTANCE = new CannotWriteException();
	private CannotWriteException() {
		super("Cannot write to database", null, false, false);
	}

	public static void throwException() throws CannotWriteException {
		throw INSTANCE;
	}

	@Override
	public synchronized Throwable fillInStackTrace() {
		return this;
	}
}
