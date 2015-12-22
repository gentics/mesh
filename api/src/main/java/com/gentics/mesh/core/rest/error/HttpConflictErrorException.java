package com.gentics.mesh.core.rest.error;

import static io.netty.handler.codec.http.HttpResponseStatus.CONFLICT;

import java.util.HashMap;

/**
 * Http 409 conflict exception
 */
public class HttpConflictErrorException extends HttpStatusCodeErrorException {

	private static final long serialVersionUID = -3129778202887838064L;

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
	public static HttpConflictErrorException conflict(String conflictingUuid, String conflictingName, String i18nMessageKey, String... parameters) {
		return new HttpConflictErrorException(conflictingUuid, conflictingName, i18nMessageKey, parameters);
	}

	/**
	 * Create a new http conflict exception.
	 * 
	 * @param conflictingUuid
	 *            Uuid of the object which was part of the conflict
	 * @param conflictingName
	 *            Name field value which caused the conflict
	 * @param message
	 *            I18n message to be attached to the exception
	 */
	public HttpConflictErrorException(String conflictingUuid, String conflictingName, String message) {
		super(CONFLICT, message);
		this.properties = new HashMap<>();
		this.properties.put("conflictingUuid", conflictingUuid);
		this.properties.put("conflictingName", conflictingName);
	}

	/**
	 * Create a new http conflict exception.
	 * 
	 * @param conflictingUuid
	 * @param conflictingName
	 * @param i18nMessageKey
	 * @param parameters
	 */
	public HttpConflictErrorException(String conflictingUuid, String conflictingName, String i18nMessageKey, String... parameters) {
		super(CONFLICT, i18nMessageKey, parameters);
		this.properties = new HashMap<>();
		this.properties.put("conflictingUuid", conflictingUuid);
		this.properties.put("conflictingName", conflictingName);
	}
}
