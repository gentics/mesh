package com.gentics.mesh.core.rest.release;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import com.gentics.mesh.core.rest.common.AbstractGenericRestResponse;

/**
 * POJO for a release response.
 */
public class ReleaseResponse extends AbstractGenericRestResponse {

	@JsonProperty(required = true)
	@JsonPropertyDescription("Name of the release")
	private String name;

	// private boolean active;

	@JsonProperty(required = true)
	@JsonPropertyDescription("Flag which indicates whether any active node migration for this release is still running or whether all nodes have been migrated to this release.")
	private boolean migrated;

	public ReleaseResponse() {
	}

	/**
	 * Set the release name.
	 * 
	 * @param name
	 * @return Fluent API
	 */
	public ReleaseResponse setName(String name) {
		this.name = name;
		return this;
	}

	/**
	 * Return the release name.
	 * 
	 * @return
	 */
	public String getName() {
		return name;
	}

	// Active flag is currently not supported
	// @JsonIgnore
	// public boolean isActive() {
	// return active;
	// }
	//
	// public void setActive(boolean active) {
	// this.active = active;
	// }

	public boolean isMigrated() {
		return migrated;
	}

	public void setMigrated(boolean migrated) {
		this.migrated = migrated;
	}
}
