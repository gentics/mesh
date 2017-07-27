package com.gentics.mesh.core.rest.microschema.impl;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import com.gentics.mesh.core.rest.common.AbstractGenericRestResponse;
import com.gentics.mesh.core.rest.schema.FieldSchema;
import com.gentics.mesh.core.rest.schema.Microschema;

/**
 * POJO for a microschema response.
 */
public class MicroschemaResponse extends AbstractGenericRestResponse implements Microschema {

	@JsonProperty(required = true)
	@JsonPropertyDescription("Version of the microschema")
	private String version;

	@JsonProperty(required = false)
	@JsonPropertyDescription("Description of the microschema")
	private String description;

	@JsonProperty(required = true)
	@JsonPropertyDescription("Name of the microschema")
	private String name;

	@JsonProperty(required = true)
	@JsonPropertyDescription("List of microschema fields")
	private List<FieldSchema> fields = new ArrayList<>();

	/**
	 * Return the container version.
	 * 
	 * @return
	 */
	public String getVersion() {
		return version;
	}

	/**
	 * Set the container version.
	 * 
	 * @param version
	 * @return Fluent API
	 */
	public MicroschemaResponse setVersion(String version) {
		this.version = version;
		return this;
	}

	@Override
	public String getDescription() {
		return description;
	}

	@Override
	public MicroschemaResponse setDescription(String description) {
		this.description = description;
		return this;
	}

	@Override
	public String getName() {
		return name;
	}

	@Override
	public MicroschemaResponse setName(String name) {
		this.name = name;
		return this;
	}

	@Override
	public List<FieldSchema> getFields() {
		return fields;
	}

	@Override
	public MicroschemaResponse setFields(List<FieldSchema> fields) {
		this.fields = fields;
		return this;
	}

	@Override
	public String toString() {
		String fields = getFields().stream().map(field -> field.getName()).collect(Collectors.joining(","));
		return getName() + " fields: {" + fields + "}";
	}

}
