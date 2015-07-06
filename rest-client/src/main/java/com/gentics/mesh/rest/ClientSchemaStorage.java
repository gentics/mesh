package com.gentics.mesh.rest;

import java.util.HashMap;
import java.util.Map;

import com.gentics.mesh.core.rest.schema.Schema;

public class ClientSchemaStorage {

	private Map<String, Schema> schemaMap = new HashMap<>();

	//	private Map<String, Microschema> microschemaMap = new HashMap<>();

	public void addSchema(Schema schema) {
		schemaMap.put(schema.getName(), schema);
	}

	public Schema getSchema(String name) {
		return schemaMap.get(name);
	}

	public void removeSchema(String name) {
		schemaMap.remove(name);
	}
}
