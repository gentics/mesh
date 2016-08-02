package com.gentics.mesh.core.rest.release;

import com.fasterxml.jackson.annotation.JsonIgnore;

public class ReleaseUpdateRequest extends ReleaseCreateRequest {

	private Boolean active;

	// The active flag is currently not supported
	@JsonIgnore
	public Boolean getActive() {
		return active;
	}

	public void setActive(Boolean active) {
		this.active = active;
	}
}
