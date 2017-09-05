package com.gentics.mesh.core.rest.schema.impl;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;

public class IndexOptions {

	@JsonProperty(required = false)
	@JsonPropertyDescription("Whether to include the raw field within the index or not. Please note that the field will be limited to 32KB of text when this setting is turned on. "
			+ "Existing values will be truncated and updates which provide values which exceed the limit will be rejected.")
	private Boolean addRaw;

	public Boolean getAddRaw() {
		return addRaw;
	}

	public IndexOptions setAddRaw(Boolean addRaw) {
		this.addRaw = addRaw;
		return this;
	}
}
