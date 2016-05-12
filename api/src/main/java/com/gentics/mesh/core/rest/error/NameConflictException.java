package com.gentics.mesh.core.rest.error;

import static io.netty.handler.codec.http.HttpResponseStatus.CONFLICT;

import java.util.HashMap;
import java.util.Map;

public class NameConflictException extends AbstractRestException {

	public static final String TYPE = "name_conflict";

	private static final long serialVersionUID = -8957392314200605121L;

	private Map<String, String> properties = new HashMap<>();

	public NameConflictException() {
	}

	/**
	 * Create a new name conflict using the provided i18n message and i18n properties.
	 * 
	 * @param i18nMessage
	 * @param i18nProperties
	 */
	public NameConflictException(String i18nMessage, String... i18nParameters) {
		super(CONFLICT, i18nMessage, i18nParameters);
	}

	public void setProperty(String key, String value) {
		this.properties.put(key, value);
	}

	@Override
	public String getType() {
		return TYPE;
	}

}
