package com.gentics.mesh.core.rest.error;

import java.util.HashMap;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import io.netty.handler.codec.http.HttpResponseStatus;

/**
 * Abstract class for regular rest exceptions. This class should be used when returning the exception information via a JSON response.
 */
@JsonIgnoreProperties({ "suppressed", "cause", "detailMessage", "stackTrace", "localizedMessage" })
public abstract class AbstractRestException extends RuntimeException {

	private static final long serialVersionUID = 2209919403583173663L;

	protected HttpResponseStatus status;
	protected String[] i18nParameters;
	protected String i18nKey;
	protected String translatedMessage;
	protected Map<String, Object> properties = new HashMap<>();

	public AbstractRestException() {
	}

	/**
	 * Create a new exception.
	 * 
	 * @param status
	 *            Status Code
	 * @param message
	 *            Message
	 * @param e
	 *            Underlying exception
	 */
	public AbstractRestException(HttpResponseStatus status, String message, Throwable e) {
		super(message, e);
		this.i18nKey = message;
		this.status = status;
	}

	/**
	 * Create a new exception.
	 * 
	 * @param status
	 *            Status code
	 * @param message
	 *            Message
	 */
	public AbstractRestException(HttpResponseStatus status, String message) {
		super(message);
		this.i18nKey = message;
		this.status = status;
	}

	/**
	 * Create a a new exception.
	 * 
	 * @param status
	 *            Http status
	 * @param i18nMessageKey
	 *            I18n message key
	 * @param i18nParameters
	 *            I18n parameters for the i18n message
	 */
	public AbstractRestException(HttpResponseStatus status, String i18nMessageKey, String... i18nParameters) {
		super(i18nMessageKey);
		this.status = status;
		this.i18nKey = i18nMessageKey;
		this.i18nParameters = i18nParameters;
	}

	/**
	 * Create a new exception.
	 * 
	 * @param message
	 *            Message
	 */
	protected AbstractRestException(String message) {
		super(message);
		this.i18nKey = message;
	}

	/**
	 * Return the http status code.
	 * 
	 * @return
	 */
	@JsonIgnore
	public HttpResponseStatus getStatus() {
		return status;
	}

	/**
	 * Set the nested http status code.
	 * 
	 * @param status
	 */
	public void setStatus(HttpResponseStatus status) {
		this.status = status;
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
	 * Returns the exception type. The type should be a human readable string which can be used to identify the error type.
	 * 
	 * @return
	 */
	public abstract String getType();

	@Override
	public String toString() {
		String extraInfo = "";
		if (getI18nParameters() != null) {
			extraInfo = " params {" + String.join(",", getI18nParameters()) + "}";
		}
		return getStatus() + " " + getI18nKey() + extraInfo;
	}

	/**
	 * Returns the stored information or if possible a translated message.
	 */
	@Override
	public String getMessage() {
		if (translatedMessage != null) {
			return translatedMessage;
		} else {
			return toString();
		}
	}

	@JsonIgnore
	public String getI18nKey() {
		return this.i18nKey;
	}

	public void setI18nKey(String i18nKey) {
		this.i18nKey = i18nKey;
	}

	@JsonIgnore
	public String getTranslatedMessage() {
		return translatedMessage;
	}

	public void setTranslatedMessage(String translatedMessage) {
		this.translatedMessage = translatedMessage;
	}

	/**
	 * Return the exception specific properties.
	 * 
	 * @return
	 */
	public Map<String, Object> getProperties() {
		return properties;
	}

	/**
	 * Return the property value for the given key.
	 * 
	 * @param key
	 *            Key of the value to be located
	 * @return Found value or null if no value was found
	 */
	public <T> T getProperty(String key) {
		return (T) getProperties().get(key);
	}

	/**
	 * Set the exception specific properties.
	 * 
	 * @param key
	 * @param value
	 */
	public void setProperty(String key, Object value) {
		this.properties.put(key, value);
	}

}
