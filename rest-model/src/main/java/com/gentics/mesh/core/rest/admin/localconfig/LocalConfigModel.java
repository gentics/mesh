package com.gentics.mesh.core.rest.admin.localconfig;

import java.io.Serializable;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import com.gentics.mesh.core.rest.common.RestModel;

public class LocalConfigModel implements RestModel, Serializable {

	@JsonProperty
	@JsonPropertyDescription("If true, mutating requests to this instance are not allowed.")
	private Boolean readOnly = false;

	public Boolean isReadOnly() {
		return readOnly;
	}

	public LocalConfigModel setReadOnly(Boolean readOnly) {
		this.readOnly = readOnly;
		return this;
	}
}
