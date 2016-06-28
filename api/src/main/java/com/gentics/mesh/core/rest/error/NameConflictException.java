package com.gentics.mesh.core.rest.error;

import static io.netty.handler.codec.http.HttpResponseStatus.CONFLICT;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import com.gentics.mesh.json.JsonUtil;

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
	public NameConflictException(String i18nMessage, String... i18nProperties) {
		super(CONFLICT, i18nMessage, i18nProperties);
	}

	/**
	 * Set the exception specific properties.
	 * 
	 * @param key
	 * @param value
	 */
	public void setProperty(String key, String value) {
		this.properties.put(key, value);
	}

	/**
	 * Return the exception specific properties.
	 * 
	 * @return
	 */
	public Map<String, String> getProperties() {
		return properties;
	}

	@Override
	public String getType() {
		return TYPE;
	}

	@Override
	public String toString() {
		if (translatedMessage != null) {
			return translatedMessage;
		} else {
			String i18nInfo = Arrays.toString(getI18nParameters());
			String propInfo = JsonUtil.toJson(getProperties());
			return "Key: " + super.getMessage() + "\n\nI18nParams:\n" + i18nInfo + "\n\nProperties:\n" + propInfo;
		}
	}

	@Override
	public String getMessage() {
		return toString();
	}

}
