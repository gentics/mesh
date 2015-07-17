package com.gentics.mesh.core.rest.schema;

public interface FieldSchema {

	String getType();

	String getLabel();

	FieldSchema setLabel(String label);

	String getName();

	FieldSchema setName(String name);

	boolean isRequired();

	FieldSchema setRequired(boolean flag);
}
