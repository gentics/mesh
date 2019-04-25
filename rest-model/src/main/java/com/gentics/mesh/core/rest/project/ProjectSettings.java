package com.gentics.mesh.core.rest.project;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;

public class ProjectSettings {

	@JsonProperty(required = false)
	@JsonPropertyDescription("Flag to enable or disable the versioning feature for the project.")
	private boolean versioning = false;

	public boolean isVersioning() {
		return versioning;
	}

	public void setVersioning(boolean versioning) {
		this.versioning = versioning;
	}
}
