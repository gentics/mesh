package com.gentics.mesh.core.rest.error;

/**
 * Exception for etag mismatch errors. 
 * The failure handler will process this exception and return a corresponding response error code. 
 */
public class NotModifiedException extends RuntimeException {

	private static final long serialVersionUID = -8156052697627682011L;
	private static final NotModifiedException instance = new NotModifiedException();

	/**
	 * Retrieve a singleton, fast {@link NotModifiedException} without a stack trace.
	 */
	public static NotModifiedException instance() {
		return instance;
	}

	@Override
	public synchronized Throwable fillInStackTrace() {
		// Don't setup the stacktrace. We don't need it.
		return this;
	}
}
