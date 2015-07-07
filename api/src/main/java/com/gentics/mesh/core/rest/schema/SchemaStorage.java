package com.gentics.mesh.core.rest.schema;

public interface SchemaStorage {

	void removeSchema(String name);

	Schema getSchema(String name);

	int size();

	void clear();

	void addSchema(Schema schema);

}
