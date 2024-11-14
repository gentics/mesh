package com.gentics.mesh.core.rest.common;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;

/**
 * General creation request data
 */
public abstract class CreateRequest implements RestModel {

	@JsonProperty(required = false)
	@JsonPropertyDescription("UUID to use, optional. Can be overridden with the query/path parameter")
	private String uuid;

	public String getUuid() {
		return uuid;
	}

	public CreateRequest setUuid(String uuid) {
		this.uuid = uuid;
		return this;
	}	
}
