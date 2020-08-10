package com.gentics.mesh.rest;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;

import com.gentics.mesh.core.rest.microschema.MicroschemaVersionModel;
import com.gentics.mesh.core.rest.schema.SchemaStorage;
import com.gentics.mesh.core.rest.schema.SchemaUpdateModel;

public class ClientSchemaStorage implements SchemaStorage {

	private Map<String, Map<String, SchemaUpdateModel>> schemaMap = new HashMap<>();

	private Map<String, Map<String, MicroschemaVersionModel>> microschemaMap = new HashMap<>();

	@Override
	public void addSchema(SchemaUpdateModel schema) {
		schemaMap.computeIfAbsent(schema.getName(), k -> new HashMap<>()).put(schema.getVersion(), schema);
	}

	@Override
	public SchemaUpdateModel getSchema(String name) {
		Optional<Entry<String, SchemaUpdateModel>> maxVersion = schemaMap.getOrDefault(name, Collections.emptyMap()).entrySet().stream()
				.max((entry1, entry2) -> {
					Double v1 = Double.valueOf(entry1.getKey());
					Double v2 = Double.valueOf(entry2.getKey());
					return Double.compare(v1, v2);
				});
		return maxVersion.isPresent() ? maxVersion.get().getValue() : null;
	}

	@Override
	public SchemaUpdateModel getSchema(String name, String version) {
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
	public MicroschemaVersionModel getMicroschema(String name) {
		Optional<Entry<String, MicroschemaVersionModel>> maxVersion = microschemaMap.getOrDefault(name, Collections.emptyMap()).entrySet().stream()
				.max((entry1, entry2) -> {
					Double v1 = Double.valueOf(entry1.getKey());
					Double v2 = Double.valueOf(entry2.getKey());
					return Double.compare(v1, v2);
				});
		return maxVersion.isPresent() ? maxVersion.get().getValue() : null;
	}

	@Override
	public MicroschemaVersionModel getMicroschema(String name, String version) {
		return microschemaMap.getOrDefault(name, Collections.emptyMap()).get(version);
	}

	@Override
	public void addMicroschema(MicroschemaVersionModel microschema) {
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
