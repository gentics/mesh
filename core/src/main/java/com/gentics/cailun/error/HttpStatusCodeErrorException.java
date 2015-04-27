package com.gentics.cailun.error;

public class HttpStatusCodeErrorException extends RuntimeException {

	private static final long serialVersionUID = 2209919403583173663L;

	/**
	 * HTTP Status code
	 */
	private int code;

	public HttpStatusCodeErrorException(int code, String message) {
		super(message);
		this.code = code;
	}

	public HttpStatusCodeErrorException(int code, String message, Throwable e) {
		super(message, e);
		this.code = code;
	}

	public int getCode() {
		return code;
	}

}
