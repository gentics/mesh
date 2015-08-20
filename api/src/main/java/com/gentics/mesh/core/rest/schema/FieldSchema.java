package com.gentics.mesh.core.rest.schema;

public interface FieldSchema {

	String getType();

	String getLabel();

	FieldSchema setLabel(String label);

	//TODO is this not the fieldkey? is the key the name? höö?
	String getName();

	FieldSchema setName(String name);

	boolean isRequired();

	FieldSchema setRequired(boolean flag);
}
