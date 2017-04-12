package com.gentics.mesh.core.rest.schema.impl;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import com.gentics.mesh.core.rest.schema.FieldSchema;
import com.gentics.mesh.core.rest.schema.Schema;
import com.gentics.mesh.core.rest.schema.SchemaModel;

/**
 * @see Schema
 */
public class SchemaModelImpl implements SchemaModel {

	@JsonProperty(required = false)
	@JsonPropertyDescription("Name of the display field.")
	private String displayField;

	@JsonProperty(required = false)
	@JsonPropertyDescription("Name of the segment field. This field is used to construct the webroot path to the node.")
	private String segmentField;

	@JsonProperty(required = false)
	@JsonPropertyDescription("Flag which indicates whether nodes which use this schema store additional child nodes.")
	private boolean container = false;

	/**
	 * Create a new schema with the given name.
	 * 
	 * @param name
	 */
	public SchemaModelImpl(String name) {
		setName(name);
	}

	public SchemaModelImpl() {
	}

	@JsonProperty(required = false)
	@JsonPropertyDescription("Version of the schema")
	private int version;

	@JsonProperty(required = false)
	@JsonPropertyDescription("Description of the schema")
	private String description;

	@JsonProperty(required = false)
	@JsonPropertyDescription("Name of the schema")
	private String name;

	@JsonProperty(required = false)
	@JsonPropertyDescription("List of schema fields")
	private List<FieldSchema> fields = new ArrayList<>();

	@Override
	public int getVersion() {
		return version;
	}

	@Override
	public SchemaModelImpl setVersion(int version) {
		this.version = version;
		return this;
	}

	@Override
	public String getDescription() {
		return description;
	}

	@Override
	public SchemaModelImpl setDescription(String description) {
		this.description = description;
		return this;
	}

	@Override
	public String getName() {
		return name;
	}

	@Override
	public SchemaModelImpl setName(String name) {
		this.name = name;
		return this;
	}

	@Override
	public String getSegmentField() {
		return segmentField;
	}

	@Override
	public SchemaModelImpl setSegmentField(String segmentField) {
		this.segmentField = segmentField;
		return this;
	}

	@Override
	public List<FieldSchema> getFields() {
		return fields;
	}

	@Override
	public SchemaModelImpl setFields(List<FieldSchema> fields) {
		this.fields = fields;
		return this;
	}

	@Override
	public boolean isContainer() {
		return container;
	}

	@Override
	public SchemaModelImpl setContainer(boolean flag) {
		this.container = flag;
		return this;
	}

	@Override
	public String getDisplayField() {
		return displayField;
	}

	@Override
	public SchemaModelImpl setDisplayField(String displayField) {
		this.displayField = displayField;
		return this;
	}

	@Override
	public String toString() {
		String fields = getFields().stream().map(field -> field.getName()).collect(Collectors.joining(","));
		return getName() + " fields: {" + fields + "}";
	}

}
