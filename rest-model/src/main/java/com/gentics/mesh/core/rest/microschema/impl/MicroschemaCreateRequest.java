package com.gentics.mesh.core.rest.microschema.impl;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import com.gentics.mesh.core.rest.schema.FieldSchema;
import com.gentics.mesh.core.rest.schema.MicroschemaModel;

import io.vertx.core.json.JsonObject;

/**
 * REST POJO for microschema create requests.
 */
public class MicroschemaCreateRequest implements MicroschemaModel {

	@JsonProperty(required = false)
	@JsonPropertyDescription("Description of the microschema")
	private String description;

	@JsonProperty(required = true)
	@JsonPropertyDescription("Name of the microschema")
	private String name;

	@JsonProperty(required = false)
	@JsonPropertyDescription("Additional elasticsearch index configuration. This can be used to setup custom analyzers and filters.")
	private JsonObject elasticsearch;

	@JsonProperty(required = false)
	@JsonPropertyDescription("'Exclude from indexing' flag.")
	private Boolean noIndex;

	@JsonPropertyDescription("List of microschema fields")
	private List<FieldSchema> fields = new ArrayList<>();

	@Override
	public String getDescription() {
		return description;
	}

	@Override
	public MicroschemaCreateRequest setDescription(String description) {
		this.description = description;
		return this;
	}

	@Override
	public Boolean getNoIndex() {
		return noIndex;
	}

	@Override
	public MicroschemaCreateRequest setNoIndex(Boolean noIndex) {
		this.noIndex = noIndex;
		return this;
	}

	@Override
	public String getName() {
		return name;
	}

	@Override
	public MicroschemaCreateRequest setName(String name) {
		this.name = name;
		return this;
	}

	@Override
	public JsonObject getElasticsearch() {
		return elasticsearch;
	}

	@Override
	public MicroschemaCreateRequest setElasticsearch(JsonObject elasticsearch) {
		this.elasticsearch = elasticsearch;
		return this;
	}

	@Override
	public List<FieldSchema> getFields() {
		return fields;
	}

	@Override
	public MicroschemaCreateRequest setFields(List<FieldSchema> fields) {
		this.fields = fields;
		return this;
	}

	@Override
	public String toString() {
		String fields = getFields().stream().map(field -> field.getName()).collect(Collectors.joining(","));
		return getName() + " fields: {" + fields + "}";
	}

}
