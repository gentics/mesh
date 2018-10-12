package com.gentics.mesh.rest.client;

import com.gentics.mesh.core.rest.common.GenericMessageResponse;

import io.vertx.core.http.HttpClientResponse;
import io.vertx.core.http.HttpMethod;

/**
 * Rest client exception which stores the error information in within a generic message response.
 */
public class MeshRestClientMessageException extends Exception {

	private static final long serialVersionUID = 6595846107882435538L;

	/**
	 * HTTP Status code
	 */
	private int statusCode;

	private GenericMessageResponse responseMessage;

	private String uri;

	private String body;

	private HttpMethod method;

	public MeshRestClientMessageException(HttpClientResponse response, String body, HttpMethod method, String uri) {
		super("Error:" + response.statusCode() + " in " + method.name() + " " + uri + " : " + response.statusMessage());
		this.statusCode = response.statusCode();
		this.body = body;
		this.uri = uri;
		this.method = method;
	}

	public MeshRestClientMessageException(HttpClientResponse response, GenericMessageResponse responseMessage, HttpMethod method, String uri) {
		super("Error:" + response.statusCode() + " in " + method.name() + " " + uri + " : " + response.statusMessage());
		this.responseMessage = responseMessage;
		this.statusCode = response.statusCode();
		this.uri = uri;
		this.method = method;
	}

	public int getStatusCode() {
		return statusCode;
	}

	public GenericMessageResponse getResponseMessage() {
		return responseMessage;
	}

	public String getBody() {
		return body;
	}

	public String getUri() {
		return uri;
	}

	public HttpMethod getMethod() {
		return method;
	}

	@Override
	public String getMessage() {
		if (responseMessage == null) {
			return super.getMessage();
		} else {
			return super.getMessage() + " Info: " + responseMessage.getMessage();
		}
	}

}
