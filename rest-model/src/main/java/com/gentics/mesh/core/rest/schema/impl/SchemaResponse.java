package com.gentics.mesh.core.rest.schema.impl;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import com.gentics.mesh.core.rest.common.AbstractGenericRestResponse;
import com.gentics.mesh.core.rest.schema.FieldSchema;
import com.gentics.mesh.core.rest.schema.SchemaVersionModel;

import io.vertx.core.json.JsonObject;

/**
 * REST model of a schema.
 */
public class SchemaResponse extends AbstractGenericRestResponse implements SchemaVersionModel {

	@JsonProperty(required = true)
	@JsonPropertyDescription("Name of the display field.")
	private String displayField;

	@JsonProperty(required = false)
	@JsonPropertyDescription("Name of the segment field. This field is used to construct the webroot path to the node.")
	private String segmentField;

	@JsonProperty(required = false)
	@JsonPropertyDescription("Names of the fields which provide a compete url to the node. This property can be used to define custom urls for certain nodes. The webroot API will try to locate the node via it's segment field and via the specified url fields.")
	private List<String> urlFields;

	@JsonProperty(required = true)
	@JsonPropertyDescription("Flag which indicates whether nodes which use this schema store additional child nodes.")
	private boolean container = false;

	@JsonProperty(required = true)
	@JsonPropertyDescription("Version of the schema.")
	private String version;

	@JsonProperty(required = false)
	@JsonPropertyDescription("Description of the schema.")
	private String description;

	@JsonProperty(required = true)
	@JsonPropertyDescription("Name of the schema.")
	private String name;

	@JsonProperty(required = false)
	@JsonPropertyDescription("Additional search index configuration. This can be used to setup custom analyzers and filters.")
	private JsonObject elasticsearch;

	@JsonProperty(required = true)
	@JsonPropertyDescription("List of schema fields")
	private List<FieldSchema> fields = new ArrayList<>();

	@JsonProperty(required = false)
	@JsonPropertyDescription("Auto purge flag of the schema. Controls whether contents of this schema should be automatically purged on update.")
	private Boolean autoPurge;

	@JsonProperty(required = false)
	@JsonPropertyDescription("Flag which indicates whether nodes which use this schema should be excluded from the indexing.")
	private Boolean noIndex;

	@Override
	public String getName() {
		return name;
	}

	@Override
	public SchemaResponse setName(String name) {
		this.name = name;
		return this;
	}

	@Override
	public Boolean getNoIndex() {
		return noIndex;
	}

	@Override
	public SchemaResponse setNoIndex(Boolean noIndex) {
		this.noIndex = noIndex;
		return this;
	}

	@Override
	public List<FieldSchema> getFields() {
		return fields;
	}

	@Override
	public SchemaResponse setFields(List<FieldSchema> fields) {
		this.fields = fields;
		return this;
	}

	@Override
	public String getSegmentField() {
		return segmentField;
	}

	@Override
	public SchemaResponse setSegmentField(String segmentField) {
		this.segmentField = segmentField;
		return this;
	}

	@Override
	public List<String> getUrlFields() {
		return urlFields;
	}

	@Override
	public SchemaResponse setUrlFields(List<String> urlFields) {
		this.urlFields = urlFields;
		return this;
	}

	@Override
	public String getDescription() {
		return description;
	}

	@Override
	public SchemaResponse setDescription(String description) {
		this.description = description;
		return this;
	}

	@Override
	public Boolean getContainer() {
		return container;
	}

	@Override
	public SchemaResponse setContainer(Boolean flag) {
		this.container = flag;
		return this;
	}

	@Override
	public String getVersion() {
		return version;
	}

	@Override
	public SchemaResponse setVersion(String version) {
		this.version = version;
		return this;
	}

	@Override
	public String getDisplayField() {
		return displayField;
	}

	@Override
	public SchemaResponse setDisplayField(String displayField) {
		this.displayField = displayField;
		return this;
	}

	/**
	 * Create a schema reference using the schema as source.
	 * 
	 * @return
	 */
	public SchemaReferenceImpl toReference() {
		SchemaReferenceImpl reference = new SchemaReferenceImpl();
		reference.setUuid(getUuid());
		reference.setVersion(getVersion());
		reference.setName(getName());
		return reference;
	}

	/**
	 * Create a new {@link SchemaUpdateRequest} using the current schema values as source.
	 * 
	 * @return
	 */
	public SchemaUpdateRequest toUpdateRequest() {
		SchemaUpdateRequest updateRequest = new SchemaUpdateRequest();
		updateRequest.setContainer(getContainer());
		updateRequest.setAutoPurge(getAutoPurge());
		updateRequest.getFields().addAll(getFields());
		updateRequest.setName(getName());
		updateRequest.setSegmentField(getSegmentField());
		updateRequest.setDisplayField(getDisplayField());
		updateRequest.setDescription(getDescription());
		updateRequest.setElasticsearch(getElasticsearch());
		updateRequest.setUrlFields(getUrlFields());
		updateRequest.setNoIndex(getNoIndex());
		return updateRequest;
	}

	@Override
	public JsonObject getElasticsearch() {
		return elasticsearch;
	}

	@Override
	public SchemaResponse setElasticsearch(JsonObject elasticsearch) {
		this.elasticsearch = elasticsearch;
		return this;
	}

	@Override
	public Boolean getAutoPurge() {
		return autoPurge;
	}

	@Override
	public SchemaResponse setAutoPurge(Boolean autoPurge) {
		this.autoPurge = autoPurge;
		return this;
	}

	@Override
	public String toString() {
		String fields = getFields().stream().map(field -> field.getName()).collect(Collectors.joining(","));
		return getName() + " fields: {" + fields + "}";
	}

}
