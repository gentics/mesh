package com.gentics.mesh.core.rest.schema.impl;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import com.gentics.mesh.core.rest.schema.FieldSchema;
import com.gentics.mesh.core.rest.schema.FieldSchemaContainer;
import com.gentics.mesh.json.MeshJsonException;

public abstract class AbstractFieldSchemaContainer implements FieldSchemaContainer {

	private int version;
	private String description;
	private String name;

	private List<FieldSchema> fields = new ArrayList<>();

	@Override
	public int getVersion() {
		return version;
	}

	@Override
	public void setVersion(int version) {
		this.version = version;
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
	public String getName() {
		return name;
	}

	@Override
	public void setName(String name) {
		this.name = name;
	}

	@Override
	public Optional<FieldSchema> getFieldSchema(String fieldName) {
		return fields.stream().filter(f -> f.getName().equals(fieldName)).findFirst();
	}

	@Override
	public FieldSchema getField(String fieldName) {
		return (FieldSchema) fields.stream().filter(f -> f.getName().equals(fieldName)).findFirst().orElse(null);
	}

	@Override
	public <T> T getField(String fieldName, Class<T> classOfT) {
		return (T) fields.stream().filter(f -> f.getName().equals(fieldName)).findFirst().orElse(null);
	}

	@Override
	public String toString() {
		String fields = getFields().stream().map(field -> field.getName()).collect(Collectors.joining(","));
		return getName() + " fields: {" + fields + "}";
	}

	@Override
	public List<FieldSchema> getFields() {
		return fields;
	}

	@Override
	public void removeField(String name) {
		if (name == null) {
			return;
		}
		fields.removeIf(field -> name.equals(field.getName()));
	}

	@Override
	public void addField(FieldSchema fieldSchema) {
		this.fields.add(fieldSchema);
	}

	@Override
	public void setFields(List<FieldSchema> fields) {
		this.fields = fields;
	}

	@Override
	public void validate() throws MeshJsonException {
		if (!getFields().stream().map(field -> field.getName()).allMatch(new HashSet<>()::add)) {
			throw new MeshJsonException("Duplicate field names detected. The name for a field must be unique.");
		}
		if (!getFields().stream().map(field -> field.getLabel()).allMatch(new HashSet<>()::add)) {
			throw new MeshJsonException("Duplicate field labels detected. The label for a field must be unique.");
		}
	}

}
