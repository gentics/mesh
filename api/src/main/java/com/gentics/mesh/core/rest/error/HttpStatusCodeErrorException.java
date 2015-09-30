package com.gentics.mesh.core.rest.error;

import com.gentics.mesh.handler.ActionContext;

import io.netty.handler.codec.http.HttpResponseStatus;
import io.vertx.core.AsyncResult;
import io.vertx.core.Future;

public class HttpStatusCodeErrorException extends RuntimeException {

	private static final long serialVersionUID = 2209919403583173663L;

	/**
	 * HTTP Status code
	 */
	private int code;

	/**
	 * Create a i18n translated error exception.
	 * 
	 * @param ac
	 * @param status
	 * @param i18nMessageKey
	 * @param parameters
	 * @return
	 */
	public static HttpStatusCodeErrorException error(ActionContext ac, HttpResponseStatus status, String i18nMessageKey, String... parameters) {
		return new HttpStatusCodeErrorException(status, ac.i18n(i18nMessageKey, parameters));
	}

	public static HttpStatusCodeErrorException error(ActionContext ac, HttpResponseStatus status, String i18nMessageKey, Throwable t) {
		return new HttpStatusCodeErrorException(status, ac.i18n(i18nMessageKey), t);
	}

	public static <T> AsyncResult<T> failedFuture(ActionContext ac, HttpResponseStatus status, String i18nMessage, Throwable cause) {
		return Future.failedFuture(new HttpStatusCodeErrorException(status, ac.i18n(i18nMessage), cause));
	}

	public static <T> AsyncResult<T> failedFuture(ActionContext ac, HttpResponseStatus status, String i18nKey, String... parameters) {
		return Future.failedFuture(new HttpStatusCodeErrorException(status, ac.i18n(i18nKey, parameters)));
	}

	public HttpStatusCodeErrorException(HttpResponseStatus status, String message, Throwable e) {
		super(message, e);
		this.code = status.code();
	}

	public HttpStatusCodeErrorException(HttpResponseStatus status, String message) {
		super(message);
		this.code = status.code();
	}

	/**
	 * Return the http status code.
	 * 
	 * @return
	 */
	public int getCode() {
		return code;
	}

}
