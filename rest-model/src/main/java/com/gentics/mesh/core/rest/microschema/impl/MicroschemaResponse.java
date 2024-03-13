package com.gentics.mesh.core.rest.microschema.impl;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import com.gentics.mesh.core.rest.common.AbstractGenericRestResponse;
import com.gentics.mesh.core.rest.schema.FieldSchema;
import com.gentics.mesh.core.rest.schema.MicroschemaModel;
import com.gentics.mesh.core.rest.schema.MicroschemaReference;
import com.gentics.mesh.core.rest.schema.impl.MicroschemaReferenceImpl;
import com.gentics.mesh.json.JsonUtil;

import io.vertx.core.json.JsonObject;

/**
 * POJO for a microschema response.
 */
public class MicroschemaResponse extends AbstractGenericRestResponse implements MicroschemaModel {

	@JsonProperty(required = true)
	@JsonPropertyDescription("Version of the microschema")
	private String version;

	@JsonProperty(required = false)
	@JsonPropertyDescription("Description of the microschema")
	private String description;

	@JsonProperty(required = true)
	@JsonPropertyDescription("Name of the microschema")
	private String name;

	@JsonProperty(required = false)
	@JsonPropertyDescription("Additional search index configuration. This can be used to setup custom analyzers and filters.")
	private JsonObject elasticsearch;

	@JsonProperty(required = false)
	@JsonPropertyDescription("'Exclude from indexing' flag.")
	private Boolean noIndex;

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
	public Boolean getNoIndex() {
		return noIndex;
	}

	@Override
	public MicroschemaResponse setNoIndex(Boolean noIndex) {
		this.noIndex = noIndex;
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
	public JsonObject getElasticsearch() {
		return elasticsearch;
	}

	@Override
	public MicroschemaResponse setElasticsearch(JsonObject elasticsearch) {
		this.elasticsearch = elasticsearch;
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

	/**
	 * Create a microschema reference using the microschema as source.
	 *
	 * @return
	 */
	public MicroschemaReference toReference() {
		MicroschemaReferenceImpl reference = new MicroschemaReferenceImpl();
		reference.setUuid(getUuid());
		reference.setVersion(getVersion());
		reference.setName(getName());
		return reference;
	}

	@Override
	public String toString() {
		String fields = getFields().stream().map(field -> field.getName()).collect(Collectors.joining(","));
		return getName() + " fields: {" + fields + "}";
	}

	/**
	 * Create a microschema update request using the microschema as source.
	 * 
	 * @return
	 */
	public MicroschemaUpdateRequest toRequest() {
		return JsonUtil.readValue(toJson(true), MicroschemaUpdateRequest.class);
	}

}
