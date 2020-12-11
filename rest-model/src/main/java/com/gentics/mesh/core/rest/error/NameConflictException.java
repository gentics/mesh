package com.gentics.mesh.core.rest.error;

import static io.netty.handler.codec.http.HttpResponseStatus.CONFLICT;

/**
 * Exception which can be used to return name conflict errors to clients.
 */
public class NameConflictException extends AbstractRestException {

	public static final String TYPE = "name_conflict";

	private static final long serialVersionUID = -8957392314200605121L;

	public NameConflictException() {
	}

	/**
	 * Create a new name conflict using the provided i18n message and i18n properties.
	 * 
	 * @param i18nMessage
	 * @param i18nProperties
	 */
	public NameConflictException(String i18nMessage, String... i18nProperties) {
		super(CONFLICT, i18nMessage, i18nProperties);
	}

	@Override
	public String getType() {
		return TYPE;
	}

}
