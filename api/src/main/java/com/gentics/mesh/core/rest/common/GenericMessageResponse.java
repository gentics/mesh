package com.gentics.mesh.core.rest.common;

import java.util.Map;

import com.gentics.mesh.core.rest.error.HttpStatusCodeErrorException;

/**
 * The {@link GenericMessageResponse} is used when a generic message should be returned to the requester.
 */
public class GenericMessageResponse {

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
	 */
	public GenericMessageResponse(String message) {
		this(message, null);
	}

	/**
	 * Create a new generic message response pojo.
	 * 
	 * @param message
	 * @param internalMessage
	 */
	public GenericMessageResponse(String message, String internalMessage) {
		this.message = message;
		this.internalMessage = internalMessage;
	}

	public GenericMessageResponse(HttpStatusCodeErrorException httpStatusError) {
		this.message = httpStatusError.getMessage();
		this.properties = httpStatusError.getProperties();
	}

	/**
	 * Return the message string.
	 * 
	 * @return
	 */
	public String getMessage() {
		return message;
	}

	/**
	 * Set the message string.
	 * 
	 * @param message
	 */
	public void setMessage(String message) {
		this.message = message;
	}

	/**
	 * Return the internal message.
	 * 
	 * @return
	 */
	public String getInternalMessage() {
		return internalMessage;
	}

	/**
	 * Set the internal message.
	 * 
	 * @param internalMessage
	 */
	public void setInternalMessage(String internalMessage) {
		this.internalMessage = internalMessage;
	}

	/**
	 * Return the additional json properties.
	 * 
	 * @return
	 */
	public Map<String,String> getProperties() {
		return properties;
	}

	/**
	 * Set additional json properties.
	 * 
	 * @param properties
	 */
	public void setProperties(Map<String,String> properties) {
		this.properties = properties;
	}
}
