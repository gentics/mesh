package com.gentics.mesh.core.rest.microschema.impl;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import com.gentics.mesh.core.rest.common.RestModel;
import com.gentics.mesh.core.rest.schema.FieldSchema;
import com.gentics.mesh.core.rest.schema.Microschema;

public class MicroschemaUpdateRequest implements RestModel, Microschema {

	@JsonPropertyDescription("Version of the microschema")
	private int version;

	@JsonPropertyDescription("Description of the microschema")
	private String description;

	@JsonPropertyDescription("Name of the microschema")
	private String name;

	@JsonPropertyDescription("List of microschema fields")
	private List<FieldSchema> fields = new ArrayList<>();

	@Override
	public int getVersion() {
		return version;
	}

	@Override
	public void setVersion(int version) {
		this.version = version;
	}

	@Override
	public String getDescription() {
		return description;
	}

	@Override
	public void setDescription(String description) {
		this.description = description;
	}

	@Override
	public String getName() {
		return name;
	}

	@Override
	public void setName(String name) {
		this.name = name;
	}

	@Override
	public List<FieldSchema> getFields() {
		return fields;
	}

	@Override
	public void setFields(List<FieldSchema> fields) {
		this.fields = fields;
	}

	@Override
	public String toString() {
		String fields = getFields().stream().map(field -> field.getName()).collect(Collectors.joining(","));
		return getName() + " fields: {" + fields + "}";
	}

}
