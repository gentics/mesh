package com.gentics.mesh.rest;

import java.util.HashMap;
import java.util.Map;

import com.gentics.mesh.core.rest.schema.Microschema;
import com.gentics.mesh.core.rest.schema.Schema;
import com.gentics.mesh.core.rest.schema.SchemaStorage;

public class ClientSchemaStorage implements SchemaStorage {

	private Map<String, Schema> schemaMap = new HashMap<>();

	private Map<String, Microschema> microschemaMap = new HashMap<>();

	@Override
	public void addSchema(Schema schema) {
		schemaMap.put(schema.getName(), schema);
	}

	@Override
	public Schema getSchema(String name) {
		return schemaMap.get(name);
	}

	@Override
	public void removeSchema(String name) {
		schemaMap.remove(name);
	}

	@Override
	public int size() {
		return schemaMap.size();
	}

	@Override
	public void clear() {
		schemaMap.clear();
		microschemaMap.clear();
	}
}
