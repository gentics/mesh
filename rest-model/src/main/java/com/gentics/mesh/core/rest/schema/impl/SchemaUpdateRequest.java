package com.gentics.mesh.core.rest.schema.impl;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import com.gentics.mesh.core.rest.schema.FieldSchema;
import com.gentics.mesh.core.rest.schema.SchemaModel;

public class SchemaUpdateRequest implements SchemaModel {

	@JsonProperty(required = false)
	@JsonPropertyDescription("Name of the display field.")
	private String displayField;

	@JsonProperty(required = false)
	@JsonPropertyDescription("Name of the segment field. This field is used to construct the webroot path to the node.")
	private String segmentField;

	@JsonProperty(required = false)
	@JsonPropertyDescription("Names of the fields which provide a compete url to the node. This property can be used to define custom urls for certain nodes. The webroot API will try to locate the node via it's segment field and via the specified url fields.")
	private List<String> urlFields;

	@JsonProperty(required = false)
	@JsonPropertyDescription("Flag which indicates whether nodes which use this schema store additional child nodes.")
	private boolean container = false;

	@JsonProperty(required = false)
	@JsonPropertyDescription("Version of the schema.")
	private String version;

	@JsonProperty(required = false)
	@JsonPropertyDescription("New description of the schema.")
	private String description;

	@JsonProperty(required = true)
	@JsonPropertyDescription("Name of the schema.")
	private String name;

	@JsonProperty(required = true)
	@JsonPropertyDescription("List of schema fields")
	private List<FieldSchema> fields = new ArrayList<>();

	@Override
	public String getName() {
		return name;
	}

	@Override
	public SchemaUpdateRequest setName(String name) {
		this.name = name;
		return this;
	}

	@Override
	public List<FieldSchema> getFields() {
		return fields;
	}

	@Override
	public SchemaUpdateRequest setFields(List<FieldSchema> fields) {
		this.fields = fields;
		return this;
	}

	@Override
	public String getSegmentField() {
		return segmentField;
	}

	@Override
	public SchemaUpdateRequest setSegmentField(String segmentField) {
		this.segmentField = segmentField;
		return this;
	}

	@Override
	public List<String> getUrlFields() {
		return urlFields;
	}

	@Override
	public SchemaUpdateRequest setUrlFields(List<String> urlFields) {
		this.urlFields = urlFields;
		return this;
	}

	@Override
	public String getDescription() {
		return description;
	}

	@Override
	public SchemaUpdateRequest setDescription(String description) {
		this.description = description;
		return this;
	}

	@Override
	public boolean isContainer() {
		return container;
	}

	@Override
	public SchemaUpdateRequest setContainer(boolean flag) {
		this.container = flag;
		return this;
	}

	@Override
	public String getVersion() {
		return version;
	}

	@Override
	public SchemaUpdateRequest setVersion(String version) {
		this.version = version;
		return this;
	}

	@Override
	public String getDisplayField() {
		return displayField;
	}

	@Override
	public SchemaUpdateRequest setDisplayField(String displayField) {
		this.displayField = displayField;
		return this;
	}

	@Override
	public String toString() {
		String fields = getFields().stream().map(field -> field.getName()).collect(Collectors.joining(","));
		return getName() + " fields: {" + fields + "}";
	}

}
