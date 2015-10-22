package com.gentics.mesh.core.rest.error;

import java.util.Map;

import com.gentics.mesh.handler.ActionContext;

import io.netty.handler.codec.http.HttpResponseStatus;
import io.vertx.core.AsyncResult;
import io.vertx.core.Future;

public class HttpStatusCodeErrorException extends RuntimeException {

	private static final long serialVersionUID = 2209919403583173663L;

	/**
	 * HTTP Status code
	 */
	protected int code;

	protected Map<String, String> properties;

	/**
	 * Create a i18n translated error exception.
	 * 
	 * @param ac
	 *            Context
	 * @param status
	 *            Http status
	 * @param i18nMessageKey
	 *            i18n key
	 * @param parameters
	 *            i18n parameters
	 * @return
	 */
	public static HttpStatusCodeErrorException error(ActionContext ac, HttpResponseStatus status, String i18nMessageKey, String... parameters) {
		return new HttpStatusCodeErrorException(status, ac.i18n(i18nMessageKey, parameters));
	}

	/**
	 * Create a i18n translated error exception.
	 * 
	 * @param ac
	 *            Context
	 * @param status
	 *            Http status
	 * @param i18nMessageKey
	 *            i18n key
	 * @param t
	 *            Nested exception
	 * @return
	 */
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

	/**
	 * Create a new http status exception
	 * 
	 * @param status
	 *            Status code
	 * @param message
	 *            Message
	 */
	public HttpStatusCodeErrorException(HttpResponseStatus status, String message) {
		super(message);
		this.code = status.code();
	}

	/**
	 * Create a new http status exception
	 * 
	 * @param message
	 *            Message
	 */
	protected HttpStatusCodeErrorException(String message) {
		super(message);
	}

	/**
	 * Return the http status code.
	 * 
	 * @return Status code
	 */
	public int getCode() {
		return code;
	}

	/**
	 * Return additional properties
	 * 
	 * @return Properties
	 */
	public Map<String, String> getProperties() {
		return properties;
	}

	/**
	 * Set additional properties which will be attached to the exception
	 * 
	 * @param properties
	 *            Properties
	 */
	public void setProperties(Map<String, String> properties) {
		this.properties = properties;
	}

}
