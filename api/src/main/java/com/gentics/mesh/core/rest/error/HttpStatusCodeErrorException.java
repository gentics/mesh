package com.gentics.mesh.core.rest.error;

import io.netty.handler.codec.http.HttpResponseStatus;

public class HttpStatusCodeErrorException extends RuntimeException {

	private static final long serialVersionUID = 2209919403583173663L;

	/**
	 * HTTP Status code
	 */
	private int code;

	@Deprecated
	public HttpStatusCodeErrorException(int code, String message) {
		super(message);
		this.code = code;
	}

	public HttpStatusCodeErrorException(HttpResponseStatus status, String message, Throwable e) {
		super(message, e);
		this.code = status.code();
	}

	public HttpStatusCodeErrorException(HttpResponseStatus status, String message) {
		super(message);
		this.code = status.code();
	}


	public int getCode() {
		return code;
	}

}
