package com.gentics.mesh.core.rest.release;

import com.gentics.mesh.core.rest.common.AbstractGenericRestResponse;

public class ReleaseResponse extends AbstractGenericRestResponse {
	private String name;

//	private boolean active;

	private boolean migrated;

	public ReleaseResponse() {
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getName() {
		return name;
	}

	// Active flag is currently not supported
//	@JsonIgnore
//	public boolean isActive() {
//		return active;
//	}
//
//	public void setActive(boolean active) {
//		this.active = active;
//	}

	public boolean isMigrated() {
		return migrated;
	}

	public void setMigrated(boolean migrated) {
		this.migrated = migrated;
	}
}
