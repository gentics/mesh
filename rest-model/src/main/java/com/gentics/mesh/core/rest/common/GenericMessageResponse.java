package com.gentics.mesh.core.rest.common;

import java.util.HashMap;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import com.gentics.mesh.core.rest.error.AbstractRestException;

/**
 * The {@link GenericMessageResponse} is used when a generic message should be returned to the requester.
 */
public class GenericMessageResponse implements RestModel {

	@JsonProperty(required = true)
	@JsonPropertyDescription("Enduser friendly translated message. Translation depends on the 'Accept-Language' header value")
	private String message;

	@JsonProperty(required = true)
	@JsonPropertyDescription("Internal developer friendly message")
	private String internalMessage;

	@JsonProperty(required = false)
	@JsonPropertyDescription("Map of i18n properties which were used to construct the provided message")
	private Map<String, Object> properties;

	/**
	 * Create a new generic message response POJO.
	 */
	public GenericMessageResponse() {
	}

	/**
	 * Create a new generic message response POJO.
	 * 
	 * @param message
	 *            Message
	 */
	public GenericMessageResponse(String message) {
		this(message, null);
	}

	/**
	 * Create a new generic message response pojo.
	 * 
	 * @param message
	 *            I18n message
	 * @param internalMessage
	 *            Internal message which may describe the message in a more technical fashion
	 */
	public GenericMessageResponse(String message, String internalMessage) {
		this.message = message;
		this.internalMessage = internalMessage;
	}

	/**
	 * Create a new generic message from a rest exception.
	 */
	public GenericMessageResponse(AbstractRestException error) {
		this(error.getTranslatedMessage(), error.getI18nKey());
		this.properties = new HashMap<>(error.getProperties());
		this.properties.put("i18nParameters", error.getI18nParameters());
	}

	/**
	 * Return the message string.
	 * 
	 * @return Message
	 */
	public String getMessage() {
		return message;
	}

	/**
	 * Set the message string.
	 * 
	 * @param message
	 *            Message
	 */
	public void setMessage(String message) {
		this.message = message;
	}

	/**
	 * Return the internal message.
	 * 
	 * @return Message
	 */
	public String getInternalMessage() {
		return internalMessage;
	}

	/**
	 * Set the internal message.
	 * 
	 * @param internalMessage
	 *            Message
	 */
	public void setInternalMessage(String internalMessage) {
		this.internalMessage = internalMessage;
	}

	/**
	 * Return the additional JSON properties.
	 * 
	 * @return JSON properties or null of no properties have been set
	 */
	public Map<String, Object> getProperties() {
		return properties;
	}

	/**
	 * Set additional JSON properties.
	 * 
	 * @param properties
	 *            JSON properties to be attached to the message
	 */
	public void setProperties(Map<String, Object> properties) {
		this.properties = properties;
	}

	/**
	 * Return the property value for the given key.
	 * 
	 * @param key
	 *            Property key
	 * @return Found value or null if no value could be found
	 */
	public Object getProperty(String key) {
		return properties.get(key);
	}

	@Override
	public String toString() {
		return getMessage();
	}
}
