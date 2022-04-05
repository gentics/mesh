package com.gentics.mesh.plugin.forms;

/**
 * An error encapsulating a HTTP response code and an error message.
 */
public class FormsError extends Exception {

	private static final long serialVersionUID = -3500581809500168742L;

	/**
	 * The desired HTTP status code for the response.
	 */
	private final int statusCode;

	/**
	 * Default constructor.
	 *
	 * @param statusCode
	 *            The HTTP status code for the response
	 * @param message
	 *            The error message
	 */
	public FormsError(int statusCode, String message) {
		super(message);

		this.statusCode = statusCode;
	}

	/**
	 * Create an instance from another error and a default status code.
	 *
	 * The status code in the response will be 500 (<code>INTERNAL_SERVER_ERROR</code>).
	 *
	 * @param cause
	 *            The actual error
	 */
	public FormsError(Throwable cause) {
		super(cause);

		this.statusCode = 500;
	}

	/**
	 * Get the HTTP status code for the response.
	 *
	 * @return The HTTP status code for the response
	 */
	public int getStatusCode() {
		return statusCode;
	}
}
