package com.gentics.mesh.core.rest.release;

import com.gentics.mesh.core.rest.common.AbstractGenericRestResponse;

/**
 * POJO for a release response.
 */
public class ReleaseResponse extends AbstractGenericRestResponse {

	private String name;

	// private boolean active;

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
