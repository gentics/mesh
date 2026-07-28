package com.gentics.mesh.plugin.forms.model.response;

/**
 * Response containing the activation status for a project
 */
public class StatusResponse {
	private boolean active;
	private boolean noIndex;

	/**
	 * True when the plugin is activated for the project, false if not
	 * @return active flag
	 */
	public boolean isActive() {
		return active;
	}

	/**
	 * Set the active flag
	 * @param active flag
	 * @return fluent API
	 */
	public StatusResponse setActive(boolean active) {
		this.active = active;
		return this;
	}

	/**
	 * Does this project ban search indexing of its forms?
	 * 
	 * @return
	 */
	public boolean isNoIndex() {
		return noIndex;
	}

	/**
	 * Set the flag of this project to ban the form search indexing
	 * 
	 * @param noIndex
	 * @return
	 */
	public StatusResponse setNoIndex(boolean noIndex) {
		this.noIndex = noIndex;
		return this;
	}
}
