package com.gentics.mesh.rest;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;

import com.gentics.mesh.core.rest.microschema.MicroschemaModel;
import com.gentics.mesh.core.rest.schema.SchemaModel;
import com.gentics.mesh.core.rest.schema.SchemaStorage;


public class ClientSchemaStorage implements SchemaStorage {

	private Map<String, Map<Integer, SchemaModel>> schemaMap = new HashMap<>();

	private Map<String, Map<Integer, MicroschemaModel>> microschemaMap = new HashMap<>();

	@Override
	public void addSchema(SchemaModel schema) {
		schemaMap.computeIfAbsent(schema.getName(), k -> new HashMap<>()).put(schema.getVersion(), schema);
	}

	@Override
	public SchemaModel getSchema(String name) {
		Optional<Entry<Integer, SchemaModel>> maxVersion = schemaMap.getOrDefault(name, Collections.emptyMap()).entrySet().stream()
				.max((entry1, entry2) -> Integer.compare(entry1.getKey(), entry2.getKey()));
		return maxVersion.isPresent() ? maxVersion.get().getValue() : null;
	}

	@Override
	public SchemaModel getSchema(String name, int version) {
		return schemaMap.getOrDefault(name, Collections.emptyMap()).get(version);
	}

	@Override
	public void removeSchema(String name) {
		schemaMap.remove(name);
	}

	@Override
	public void removeSchema(String name, int version) {
		if (schemaMap.containsKey(name)) {
			schemaMap.get(name).remove(version);
		}
	}

	@Override
	public MicroschemaModel getMicroschema(String name) {
		Optional<Entry<Integer, MicroschemaModel>> maxVersion = microschemaMap.getOrDefault(name, Collections.emptyMap()).entrySet().stream()
				.max((entry1, entry2) -> Integer.compare(entry1.getKey(), entry2.getKey()));
		return maxVersion.isPresent() ? maxVersion.get().getValue() : null;
	}

	@Override
	public MicroschemaModel getMicroschema(String name, int version) {
		return microschemaMap.getOrDefault(name, Collections.emptyMap()).get(version);
	}

	@Override
	public void addMicroschema(MicroschemaModel microschema) {
		microschemaMap.computeIfAbsent(microschema.getName(), k -> new HashMap<>()).put(microschema.getVersion(), microschema);
	}

	@Override
	public void removeMicroschema(String name) {
		microschemaMap.remove(name);
	}

	@Override
	public void removeMicroschema(String name, int version) {
		if (microschemaMap.containsKey(name)) {
			microschemaMap.get(name).remove(version);
		}
	}

	@Override
	public int size() {
		return schemaMap.size() + microschemaMap.size();
	}

	@Override
	public void clear() {
		schemaMap.clear();
		microschemaMap.clear();
	}
}
