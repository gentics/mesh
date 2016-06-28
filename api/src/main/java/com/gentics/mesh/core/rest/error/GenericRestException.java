package com.gentics.mesh.core.rest.error;

import java.util.Arrays;

import io.netty.handler.codec.http.HttpResponseStatus;

/**
 * A generic rest exception which does not include any additional properties.
 */
public class GenericRestException extends AbstractRestException {

	public static final String TYPE = "generic_error";

	private static final long serialVersionUID = -5827338436269302933L;

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
	public GenericRestException(HttpResponseStatus status, String message, Throwable e) {
		super(status, message, e);
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
	public GenericRestException(HttpResponseStatus status, String message) {
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
	public GenericRestException(HttpResponseStatus status, String i18nMessageKey, String... i18nParameters) {
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
	protected GenericRestException(String message) {
		super(message);
	}

	@Override
	public String getType() {
		return TYPE;
	}

	@Override
	public String getMessage() {
		return super.getMessage() + "," + Arrays.toString(getI18nParameters());
	}

}
