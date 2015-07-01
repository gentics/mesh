package com.gentics.mesh.core.rest.schema.impl;

import java.util.HashMap;
import java.util.Map;

import com.gentics.mesh.core.rest.schema.FieldSchema;
import com.gentics.mesh.core.rest.schema.Schema;

public class SchemaImpl implements Schema {

	private String name;
	private String description;
	private String displayField;
	private boolean binary = false;
	private boolean container = false;

	private String meshVersion;

	private String schemaVersion;
	private Map<String, FieldSchema> fields = new HashMap<>();

	@Override
	public String getName() {
		return name;
	}

	@Override
	public void setName(String name) {
		this.name = name;
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
	public String getDisplayField() {
		return displayField;
	}

	@Override
	public void setDisplayField(String displayField) {
		this.displayField = displayField;
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
	public boolean isBinary() {
		return binary;
	}

	@Override
	public void setBinary(boolean flag) {
		this.binary = flag;
	}

	@Override
	public Map<String, FieldSchema> getFields() {
		return fields;
	}

	@Override
	public String getSchemaVersion() {
		return schemaVersion;
	}

	@Override
	public void setSchemaVersion(String version) {
		schemaVersion = version;
	}

	@Override
	public String getMeshVersion() {
		return meshVersion;
	}

	@Override
	public void setMeshVersion(String meshVersion) {
		this.meshVersion = meshVersion;
	}

	@Override
	public void addField(String key, FieldSchema fieldSchema) {
		this.fields.put(key, fieldSchema);
	}

}
