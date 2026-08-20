package com.gentics.mesh.core.rest.common;

import java.util.Map;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import com.gentics.vertx.openapi.model.MessageResponse;

/**
 * The {@link GenericMessageResponse} is used when a generic message should be returned to the requester.
 */
public class GenericMessageResponse extends MessageResponse implements RestModel {

	@JsonProperty(required = false)
	@JsonPropertyDescription("Internal developer friendly message")
	private String internalMessage;

	@JsonProperty(required = false)
	@JsonPropertyDescription("Map of i18n properties which were used to construct the provided message")
	private Map<String, Object> properties;

	/**
	 * Create a new generic message response POJO.
	 */
	public GenericMessageResponse() {
		super();
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
		super(message);
		this.internalMessage = internalMessage;
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
