package com.gentics.mesh.core.rest.schema.request;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.gentics.mesh.core.rest.common.response.AbstractRestModel;

public class SchemaUpdateRequest extends AbstractRestModel {

	private final String type = "object";

	@JsonProperty("title")
	private String name;

	private String description;

	private String displayName;

	public SchemaUpdateRequest() {
	}

	public SchemaUpdateRequest(String name) {
		this.name = name;
	}

	public String getType() {
		return type;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getDescription() {
		return description;
	}

	public void setDescription(String description) {
		this.description = description;
	}

	public String getDisplayName() {
		return displayName;
	}

	public void setDisplayName(String displayName) {
		this.displayName = displayName;
	}

}
