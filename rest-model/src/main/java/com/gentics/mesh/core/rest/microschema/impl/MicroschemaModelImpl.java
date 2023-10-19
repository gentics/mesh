package com.gentics.mesh.core.rest.microschema.impl;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import com.gentics.mesh.core.rest.microschema.MicroschemaVersionModel;
import com.gentics.mesh.core.rest.schema.FieldSchema;

import io.vertx.core.json.JsonObject;

/**
 * Implementation of Microschema
 */
public class MicroschemaModelImpl implements MicroschemaVersionModel {

	@JsonProperty(required = false)
	@JsonPropertyDescription("Version of the microschema")
	private String version;

	@JsonProperty(required = false)
	@JsonPropertyDescription("Description of the microschema")
	private String description;

	@JsonProperty(required = true)
	@JsonPropertyDescription("Name of the microschema")
	private String name;

	@JsonProperty(required = false)
	@JsonPropertyDescription("Additional elasticsearch index configuration. This can be used to setup custom analyzers and filters.")
	private JsonObject elasticsearch;

	@JsonProperty(required = true)
	@JsonPropertyDescription("List of microschema fields")
	private List<FieldSchema> fields = new ArrayList<>();

	@JsonProperty(required = false)
	@JsonPropertyDescription("'Exclude from indexing' flag.")
	private Boolean noIndex;

	@Override
	public String getVersion() {
		return version;
	}

	@Override
	public MicroschemaVersionModel setVersion(String version) {
		this.version = version;
		return this;
	}

	@Override
	public String getDescription() {
		return description;
	}

	@Override
	public MicroschemaVersionModel setDescription(String description) {
		this.description = description;
		return this;
	}

	@Override
	public Boolean getNoIndex() {
		return noIndex;
	}

	@Override
	public MicroschemaVersionModel setNoIndex(Boolean noIndex) {
		this.noIndex = noIndex;
		return this;
	}

	@Override
	public String getName() {
		return name;
	}

	@Override
	public MicroschemaVersionModel setName(String name) {
		this.name = name;
		return this;
	}

	@Override
	public JsonObject getElasticsearch() {
		return elasticsearch;
	}

	@Override
	public MicroschemaVersionModel setElasticsearch(JsonObject elasticsearch) {
		this.elasticsearch = elasticsearch;
		return this;
	}

	@Override
	public List<FieldSchema> getFields() {
		return fields;
	}

	@Override
	public MicroschemaVersionModel setFields(List<FieldSchema> fields) {
		this.fields = fields;
		return this;
	}

	@Override
	public String toString() {
		String fields = getFields().stream().map(field -> field.getName()).collect(Collectors.joining(","));
		return getName() + " fields: {" + fields + "}";
	}

}
