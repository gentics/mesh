package com.gentics.cailun.core.rest.response;

import java.util.ArrayList;
import java.util.List;

public class RestObjectSchema extends AbstractRestModel {

	private String name;

	private String description;

	private List<RestPropertyTypeSchema> propertyTypeSchemas = new ArrayList<>();

	public RestObjectSchema() {
	}

	public RestObjectSchema(String name) {
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

	public List<RestPropertyTypeSchema> getPropertyTypeSchemas() {
		return propertyTypeSchemas;
	}

	public void setPropertyTypeSchemas(List<RestPropertyTypeSchema> propertyTypeSchemas) {
		this.propertyTypeSchemas = propertyTypeSchemas;
	}

}
