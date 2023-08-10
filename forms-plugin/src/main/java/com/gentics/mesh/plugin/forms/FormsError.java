package com.gentics.mesh.plugin.forms;

import com.gentics.mesh.plugin.common.MeshPluginException;

/**
 * An error encapsulating a HTTP response code and an error message.
 */
public class FormsError extends MeshPluginException {

	private static final long serialVersionUID = -2794130275517608113L;

	/**
	 * Default constructor.
	 *
	 * @param message The error message
	 * @param statusCode The HTTP status code for the response
	 */
	public FormsError(String message, int statusCode) {
		super(message, statusCode);
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
	}
}
