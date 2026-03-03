package com.gentics.mesh.plugin.forms.model.response;

import com.gentics.mesh.core.rest.common.RestModel;

/**
 * Response containing the activation status for a project
 */
public class StatusResponse implements RestModel {
	private boolean active;

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
}
