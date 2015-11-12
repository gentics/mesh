package com.gentics.mesh.core.rest.error;

import java.util.HashMap;

import com.gentics.mesh.handler.ActionContext;

import io.netty.handler.codec.http.HttpResponseStatus;

/**
 * Http 409 conflict exception
 */
public class HttpConflictErrorException extends HttpStatusCodeErrorException {

	private static final long serialVersionUID = -3129778202887838064L;

	/**
	 * Create a new http conflict exception.
	 * 
	 * @param ac
	 *            Context which will be used to translate the i18n message
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
	public static HttpConflictErrorException conflict(ActionContext ac, String conflictingUuid, String conflictingName, String i18nMessageKey,
			String... parameters) {
		return new HttpConflictErrorException(ac.i18n(i18nMessageKey, parameters), conflictingUuid, conflictingName);
	}

	/**
	 * Create a new http conflict exception.
	 * 
	 * @param message
	 *            I18n message to be attached to the exception
	 * @param conflictingUuid
	 *            Uuid of the object which was part of the conflict
	 * @param conflictingName
	 *            Name field value which caused the conflict
	 */
	public HttpConflictErrorException(String message, String conflictingUuid, String conflictingName) {
		super(message);
		this.status = HttpResponseStatus.CONFLICT;
		this.properties = new HashMap<>();
		this.properties.put("conflictingUuid", conflictingUuid);
		this.properties.put("conflictingName", conflictingName);
	}
}
