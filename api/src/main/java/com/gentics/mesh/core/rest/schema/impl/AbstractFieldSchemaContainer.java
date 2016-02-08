package com.gentics.mesh.core.rest.schema.impl;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import com.gentics.mesh.core.rest.schema.FieldSchema;
import com.gentics.mesh.core.rest.schema.FieldSchemaContainer;

public abstract class AbstractFieldSchemaContainer implements FieldSchemaContainer {

	private String name;

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
	public Optional<FieldSchema> getFieldSchema(String fieldName) {
		return fields.stream().filter(f -> f.getName().equals(fieldName)).findFirst();
	}

	@Override
	public FieldSchema getField(String fieldName) {
		return (FieldSchema)fields.stream().filter(f -> f.getName().equals(fieldName)).findFirst().orElse(null);
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

}
