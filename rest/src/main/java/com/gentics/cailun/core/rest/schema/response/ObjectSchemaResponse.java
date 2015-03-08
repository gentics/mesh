package com.gentics.cailun.core.rest.schema.response;

import java.util.ArrayList;
import java.util.List;

import com.gentics.cailun.core.rest.common.response.AbstractRestModel;

public class ObjectSchemaResponse extends AbstractRestModel {

	private String name;

	private String description;

	private List<PropertyTypeSchemaResponse> propertyTypeSchemas = new ArrayList<>();

	public ObjectSchemaResponse() {
	}

	public ObjectSchemaResponse(String name) {
		this.name = name;
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
