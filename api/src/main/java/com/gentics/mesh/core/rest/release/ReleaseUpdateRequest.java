package com.gentics.mesh.core.rest.release;

public class ReleaseUpdateRequest extends ReleaseCreateRequest {
	private Boolean active;

	public Boolean getActive() {
		return active;
	}

	public void setActive(Boolean active) {
		this.active = active;
	}
}
