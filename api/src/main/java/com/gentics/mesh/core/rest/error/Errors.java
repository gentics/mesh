package com.gentics.mesh.core.rest.error;

import static io.netty.handler.codec.http.HttpResponseStatus.CONFLICT;

import java.util.HashMap;

import io.netty.handler.codec.http.HttpResponseStatus;

public final class Errors {

	/**
	 * Create a new http conflict exception.
	 * 
	 * @param conflictingUuid
	 *            Uuid of the object which was part of the conflict
	 * @param conflictingName
	 *            Name field value which caused the conflict
	 * @param i18nMessageKey
	 *            I18n key
	 * @param parameters
	 *            I18n message parameters
	 * @return
	 */
	public static HttpStatusCodeErrorException conflict(String conflictingUuid, String conflictingName, String i18nMessageKey, String... parameters) {
		HttpStatusCodeErrorException error = new HttpStatusCodeErrorException(CONFLICT, i18nMessageKey, parameters);
		error.setProperties(new HashMap<>());
		error.setProperty("conflictingUuid", conflictingUuid);
		error.setProperty("conflictingName", conflictingName);
		return error;
	}

	/**
	 * Create a i18n translated error exception.
	 * 
	 * @param status
	 *            Http status
	 * @param i18nMessageKey
	 *            i18n key
	 * @param parameters
	 *            i18n parameters
	 * @return
	 */
	public static HttpStatusCodeErrorException error(HttpResponseStatus status, String i18nMessageKey, String... parameters) {
		return new HttpStatusCodeErrorException(status, i18nMessageKey, parameters);
	}

	/**
	 * Create a i18n translated error exception.
	 * 
	 * @param status
	 *            Http status
	 * @param i18nMessageKey
	 *            i18n key
	 * @param t
	 *            Nested exception
	 * @return
	 */
	public static HttpStatusCodeErrorException error(HttpResponseStatus status, String i18nMessageKey, Throwable t) {
		return new HttpStatusCodeErrorException(status, i18nMessageKey, t);
	}

}
