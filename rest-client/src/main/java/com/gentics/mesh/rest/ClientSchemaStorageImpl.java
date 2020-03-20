package com.gentics.mesh.rest;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;

import com.gentics.mesh.core.rest.microschema.MicroschemaModel;
import com.gentics.mesh.core.rest.schema.FieldSchemaContainer;
import com.gentics.mesh.core.rest.schema.SchemaModel;
import com.gentics.mesh.core.rest.schema.ServerSchemaStorage;

public class ClientSchemaStorageImpl implements ServerSchemaStorage {

	private Map<String, Map<String, SchemaModel>> schemaMap = new HashMap<>();

	private Map<String, Map<String, MicroschemaModel>> microschemaMap = new HashMap<>();

	@Override
	public void init() {

	}

	@Override
	public void addSchema(SchemaModel schema) {
		schemaMap.computeIfAbsent(schema.getName(), k -> new HashMap<>()).put(schema.getVersion(), schema);
	}

	@Override
	public SchemaModel getSchema(String name) {
		Optional<Entry<String, SchemaModel>> maxVersion = schemaMap.getOrDefault(name, Collections.emptyMap()).entrySet().stream()
			.max((entry1, entry2) -> {
				Double v1 = Double.valueOf(entry1.getKey());
				Double v2 = Double.valueOf(entry2.getKey());
				return Double.compare(v1, v2);
			});
		return maxVersion.isPresent() ? maxVersion.get().getValue() : null;
	}

	@Override
	public SchemaModel getSchema(String name, String version) {
		return schemaMap.getOrDefault(name, Collections.emptyMap()).get(version);
	}

	@Override
	public void removeSchema(String name) {
		schemaMap.remove(name);
	}

	@Override
	public void removeSchema(String name, String version) {
		if (schemaMap.containsKey(name)) {
			schemaMap.get(name).remove(version);
		}
	}

	@Override
	public MicroschemaModel getMicroschema(String name) {
		Optional<Entry<String, MicroschemaModel>> maxVersion = microschemaMap.getOrDefault(name, Collections.emptyMap()).entrySet().stream()
			.max((entry1, entry2) -> {
				Double v1 = Double.valueOf(entry1.getKey());
				Double v2 = Double.valueOf(entry2.getKey());
				return Double.compare(v1, v2);
			});
		return maxVersion.isPresent() ? maxVersion.get().getValue() : null;
	}

	@Override
	public MicroschemaModel getMicroschema(String name, String version) {
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
	public void removeMicroschema(String name, String version) {
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
