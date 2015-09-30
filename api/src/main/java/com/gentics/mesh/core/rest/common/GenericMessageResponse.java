package com.gentics.mesh.core.rest.common;

/**
 * The {@link GenericMessageResponse} is used when a generic message should be returned to the requester.
 */
public class GenericMessageResponse {

	private String message;

	private String internalMessage;

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
}
