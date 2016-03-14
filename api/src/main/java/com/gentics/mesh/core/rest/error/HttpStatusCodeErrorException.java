package com.gentics.mesh.core.rest.error;

import java.util.HashMap;
import java.util.Map;

import io.netty.handler.codec.http.HttpResponseStatus;

public class HttpStatusCodeErrorException extends RuntimeException {

	private static final long serialVersionUID = 2209919403583173663L;

	protected HttpResponseStatus status;
	protected String[] i18nParameters;

	protected Map<String, String> properties;

	/**
	 * Create a new http status exception.
	 * 
	 * @param status
	 *            Status Code
	 * @param message
	 *            Message
	 * @param e
	 *            Underlying exception
	 */
	public HttpStatusCodeErrorException(HttpResponseStatus status, String message, Throwable e) {
		super(message, e);
		this.status = status;
	}

	/**
	 * Create a new http status exception.
	 * 
	 * @param status
	 *            Status code
	 * @param message
	 *            Message
	 */
	public HttpStatusCodeErrorException(HttpResponseStatus status, String message) {
		super(message);
		this.status = status;
	}

	/**
	 * Create a a new i18n exception.
	 * 
	 * @param status
	 *            Http status
	 * @param i18nMessageKey
	 *            I18n message key
	 * @param i18nParameters
	 *            I18n parameters for the i18n message
	 */
	public HttpStatusCodeErrorException(HttpResponseStatus status, String i18nMessageKey, String... i18nParameters) {
		super(i18nMessageKey);
		this.status = status;
		this.i18nParameters = i18nParameters;
	}

	/**
	 * Create a new http status exception.
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
	 * @return
	 */
	public HttpResponseStatus getStatus() {
		return status;
	}

	/**
	 * Return the i18n parameters for the error message.
	 * 
	 * @return
	 */
	public String[] getI18nParameters() {
		return i18nParameters;
	}

	/**
	 * Return additional properties.
	 * 
	 * @return Properties
	 */
	public Map<String, String> getProperties() {
		return properties;
	}

	/**
	 * Set the given property to the error.
	 * 
	 * @param key
	 * @param value
	 */
	public void setProperty(String key, String value) {
		if (properties == null) {
			properties = new HashMap<>();
		}
		properties.put(key, value);
	}

	/**
	 * Set additional properties which will be attached to the exception.
	 * 
	 * @param properties
	 *            Properties
	 */
	public void setProperties(Map<String, String> properties) {
		this.properties = properties;
	}

	@Override
	public String toString() {
		String extraInfo = "";
		if (getI18nParameters() != null) {
			extraInfo = " params {" + String.join(",", getI18nParameters()) + "}";
		}
		return getStatus() + " " + getMessage() + extraInfo;
	}

}
