package com.gentics.cailun.core.rest.schema.response;

import java.util.ArrayList;
import java.util.List;

import javax.annotation.PropertyKey;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.gentics.cailun.core.rest.common.response.AbstractRestModel;

public class ObjectSchemaResponse extends AbstractRestModel {

	@JsonProperty("$schema")
	private final String schemaVersion = "http://json-schema.org/draft-04/schema#";

	private final String type = "object";

	@JsonProperty("title")
	private String name;

	private String description;

	@JsonProperty("properties")
	private List<PropertyTypeSchemaResponse> propertyTypeSchemas = new ArrayList<>();

	public ObjectSchemaResponse() {
	}

	public String getSchemaVersion() {
		return schemaVersion;
	}

	public ObjectSchemaResponse(String name) {
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

	public List<PropertyTypeSchemaResponse> getPropertyTypeSchemas() {
		return propertyTypeSchemas;
	}

	public void setPropertyTypeSchemas(List<PropertyTypeSchemaResponse> propertyTypeSchemas) {
		this.propertyTypeSchemas = propertyTypeSchemas;
	}

}
