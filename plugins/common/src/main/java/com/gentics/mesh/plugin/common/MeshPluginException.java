package com.gentics.mesh.plugin.common;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import com.gentics.mesh.core.rest.common.RestModel;
import io.netty.handler.codec.http.HttpResponseStatus;

/**
 * Base for exceptions thrown by Mesh plugins, which provides an HTTP status
 * code in addition to the error message.
 */
public class MeshPluginException extends Exception implements RestModel {

	private static final long serialVersionUID = -8867074854068854584L;

	@JsonProperty
	@JsonPropertyDescription("The HTTP status code to send when this exception occurs")
	private int statusCode;

	public MeshPluginException() {
		this(HttpResponseStatus.INTERNAL_SERVER_ERROR.code());
	}

	public MeshPluginException(int statusCode) {
		super();

		this.statusCode = statusCode;
	}

	public MeshPluginException(String message) {
		this(message, HttpResponseStatus.INTERNAL_SERVER_ERROR.code());
	}

	public MeshPluginException(Throwable cause) {
		this(cause, HttpResponseStatus.INTERNAL_SERVER_ERROR.code());
	}

	public MeshPluginException(String message, int statusCode) {
		super(message);

		this.statusCode = statusCode;
	}

	public MeshPluginException(Throwable cause, int statusCode) {
		super(cause);

		this.statusCode = statusCode;
	}

	public MeshPluginException(String message, Throwable cause, int statusCode) {
		super(message, cause);

		this.statusCode = statusCode;
	}

	public int getStatusCode() {
		return statusCode;
	}

	public void setStatusCode(int statusCode) {
		this.statusCode = statusCode;
	}
}
