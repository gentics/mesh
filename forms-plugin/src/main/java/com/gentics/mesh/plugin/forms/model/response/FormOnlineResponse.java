package com.gentics.mesh.plugin.forms.model.response;

/**
 * Response containing the online status of a form
 */
public class FormOnlineResponse {
	private boolean online;

	/**
	 * True when the form is currently online
	 * @return online status
	 */
	public boolean isOnline() {
		return online;
	}

	/**
	 * Set the online status of the form
	 * @param online status
	 * @return fluent API
	 */
	public FormOnlineResponse setOnline(boolean online) {
		this.online = online;
		return this;
	}
}
