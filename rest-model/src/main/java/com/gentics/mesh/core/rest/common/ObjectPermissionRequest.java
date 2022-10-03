package com.gentics.mesh.core.rest.common;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;

public class ObjectPermissionRequest extends ObjectPermissionResponse {
	@JsonProperty(required = false, defaultValue = "false")
	@JsonPropertyDescription("Flag which indicates whether the permissions granted to only the given roles (will be revoked from all other roles).")
	private boolean exclusive = false;

	/**
	 * Flag that indicated that the request should be executed exclusively.
	 *
	 * @return Flag value
	 */
	public boolean isExclusive() {
		return exclusive;
	}

	/**
	 * Set the flag which indicated whether the permission changes should be applied exclusively.
	 *
	 * @param exclusive
	 *            Flag value
	 * @return Fluent API
	 */
	public ObjectPermissionRequest setExclusive(boolean exclusive) {
		this.exclusive = exclusive;
		return this;
	}
}
