package com.gentics.mesh.core.rest.schema.impl;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import com.gentics.mesh.core.rest.common.AbstractGenericRestResponse;
import com.gentics.mesh.core.rest.schema.FieldSchema;
import com.gentics.mesh.core.rest.schema.SchemaModel;

public class SchemaResponse extends AbstractGenericRestResponse implements SchemaModel {

	@JsonProperty(required = true)
	@JsonPropertyDescription("Name of the display field.")
	private String displayField;

	@JsonProperty(required = true)
	@JsonPropertyDescription("Name of the segment field. This field is used to construct the webroot path to the node.")
	private String segmentField;

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

	@JsonProperty(required = true)
	@JsonPropertyDescription("List of schema fields")
	private List<FieldSchema> fields = new ArrayList<>();

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
	public String getDescription() {
		return description;
	}

	@Override
	public SchemaResponse setDescription(String description) {
		this.description = description;
		return this;
	}

	@Override
	public boolean isContainer() {
		return container;
	}

	@Override
	public SchemaResponse setContainer(boolean flag) {
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

	@Override
	public String toString() {
		String fields = getFields().stream().map(field -> field.getName()).collect(Collectors.joining(","));
		return getName() + " fields: {" + fields + "}";
	}
}
