package com.gentics.mesh.core.rest.common;

import java.util.Map;

import com.gentics.mesh.handler.ActionContext;

/**
 * The {@link GenericMessageResponse} is used when a generic message should be returned to the requester.
 */
public class GenericMessageResponse implements RestModel {

	private String message;

	private String internalMessage;

	private Map<String, String> properties;

	/**
	 * Create a new generic message response pojo.
	 */
	public GenericMessageResponse() {
	}

	/**
	 * Create a new generic message response pojo.
	 * 
	 * @param message
	 *            Message
	 */
	public GenericMessageResponse(String message) {
		this(message, null);
	}

	/**
	 * Generate a new message response.
	 * 
	 * @param ac
	 * @param i18nMessage
	 * @param i18nParameters
	 * @return
	 */
	public static GenericMessageResponse message(ActionContext ac, String i18nMessage, String... i18nParameters) {
		return new GenericMessageResponse(ac.i18n(i18nMessage, i18nParameters));
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

	public GenericMessageResponse(String message, String internalMessage, Map<String, String> properties) {
		this(message, internalMessage);
		this.properties = properties;
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
	public Map<String, String> getProperties() {
		return properties;
	}

	/**
	 * Set additional JSON properties.
	 * 
	 * @param properties
	 *            JSON properties to be attached to the message
	 */
	public void setProperties(Map<String, String> properties) {
		this.properties = properties;
	}

	@Override
	public String toString() {
		return getMessage();
	}
}
