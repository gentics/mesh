package com.gentics.mesh.core.rest.schema.impl;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import com.gentics.mesh.core.rest.common.AbstractGenericRestResponse;
import com.gentics.mesh.core.rest.schema.FieldSchema;
import com.gentics.mesh.core.rest.schema.Schema;
import com.gentics.mesh.core.rest.schema.SchemaReference;

public class SchemaResponse extends AbstractGenericRestResponse implements Schema {

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
	@JsonPropertyDescription("Version of the schema")
	private int version;

	@JsonProperty(required = false)
	@JsonPropertyDescription("Description of the schema")
	private String description;

	@JsonProperty(required = true)
	@JsonPropertyDescription("Name of the schema")
	private String name;

	@JsonProperty(required = true)
	@JsonPropertyDescription("List of schema fields")
	private List<FieldSchema> fields = new ArrayList<>();

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
	public String getSegmentField() {
		return segmentField;
	}

	@Override
	public void setSegmentField(String segmentField) {
		this.segmentField = segmentField;
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
	public boolean isContainer() {
		return container;
	}

	@Override
	public void setContainer(boolean flag) {
		this.container = flag;
	}

	@Override
	public int getVersion() {
		return version;
	}

	@Override
	public void setVersion(int version) {
		this.version = version;
	}

	@Override
	public String getDisplayField() {
		return displayField;
	}

	@Override
	public void setDisplayField(String displayField) {
		this.displayField = displayField;
	}

	/**
	 * Create a schema reference using the schema as source.
	 * 
	 * @return
	 */
	public SchemaReference toReference() {
		SchemaReference reference = new SchemaReference();
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
