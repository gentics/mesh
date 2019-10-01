package com.gentics.mesh.plugin.like.error;

import io.netty.handler.codec.http.HttpResponseStatus;

/**
 * An error encapsulating a HTTP response code and an error message.
 */
public class LikePluginError extends RuntimeException {
	/**
	 * Serial Version UID
	 */
	private static final long serialVersionUID = 8862901746842901994L;

	/**
	 * The desired HTTP status code for the response.
	 */
	private final int statusCode;

	/**
	 * Construct an instance from an error message.
	 *
	 * The status code will be set to {@link HttpResponseStatus#INTERNAL_SERVER_ERROR}.
	 *
	 * @param message The error message
	 */
	public LikePluginError(String message) {
		super(message);

		statusCode = HttpResponseStatus.INTERNAL_SERVER_ERROR.code();
	}

	/**
	 * Default constructor.
	 *
	 * @param statusCode The HTTP status code for the response
	 * @param message The error message
	 */
	public LikePluginError(int statusCode, String message) {
		super(message);

		this.statusCode = statusCode;
	}

	/**
	 * Get the HTTP status code for the response.
	 *
	 * @return The HTTP status code for the response
	 */
	public int getHttpStatusCode() {
		return statusCode;
	}
}
