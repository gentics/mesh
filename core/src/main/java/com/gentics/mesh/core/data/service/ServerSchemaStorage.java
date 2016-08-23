package com.gentics.mesh.core.data.service;

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;

import javax.inject.Inject;

import com.gentics.mesh.cli.BootstrapInitializer;
import com.gentics.mesh.core.data.schema.MicroschemaContainerVersion;
import com.gentics.mesh.core.data.schema.SchemaContainerVersion;
import com.gentics.mesh.core.rest.schema.FieldSchemaContainer;
import com.gentics.mesh.core.rest.schema.Microschema;
import com.gentics.mesh.core.rest.schema.Schema;
import com.gentics.mesh.core.rest.schema.SchemaStorage;

import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;

public class ServerSchemaStorage implements SchemaStorage {

	private static final Logger log = LoggerFactory.getLogger(ServerSchemaStorage.class);

	public static ServerSchemaStorage instance;

	public static ServerSchemaStorage getInstance() {
		return instance;
	}

	private BootstrapInitializer boot;

	/**
	 * Map holding the schemas per name and version
	 */
	private Map<String, Map<Integer, Schema>> schemas = new HashMap<>();

	private Map<String, Map<Integer, Microschema>> microschemas = new HashMap<>();

	@Inject
	public ServerSchemaStorage(BootstrapInitializer boot) {
		this.boot = boot;
		instance = this;
	}

	public void init() {
		//Iterate over all schemas and load them into the storage
		boot.schemaContainerRoot().findAll().stream().forEach(container -> {
			for (SchemaContainerVersion version : container.findAll()) {
				Schema restSchema = version.getSchema();
				schemas.computeIfAbsent(restSchema.getName(), k -> new HashMap<>()).put(restSchema.getVersion(), restSchema);
			}
		});

		// load all microschemas and add to storage
		boot.microschemaContainerRoot().findAll().stream().forEach(container -> {
			for (MicroschemaContainerVersion version : container.findAll()) {
				Microschema restMicroschema = version.getSchema();
				microschemas.computeIfAbsent(restMicroschema.getName(), k -> new HashMap<>()).put(restMicroschema.getVersion(), restMicroschema);
			}
		});
	}

	@Override
	public void clear() {
		schemas.clear();
		microschemas.clear();
	}

	@Override
	public int size() {
		return schemas.size() + microschemas.size();
	}

	@Override
	public Schema getSchema(String name) {
		Map<Integer, Schema> schemaMap = schemas.get(name);
		if (schemaMap == null) {
			return null;
		}
		Optional<Entry<Integer, Schema>> maxVersion = schemaMap.entrySet().stream()
				.max((entry1, entry2) -> Integer.compare(entry1.getKey(), entry2.getKey()));
		if (maxVersion.isPresent()) {
			return maxVersion.get().getValue();
		} else {
			return null;
		}
	}

	@Override
	public Schema getSchema(String name, int version) {
		Map<Integer, Schema> schemaMap = schemas.get(name);
		if (schemaMap == null) {
			return null;
		} else {
			return schemaMap.get(version);
		}
	}

	@Override
	public void removeSchema(String name) {
		schemas.remove(name);
	}

	@Override
	public void removeSchema(String name, int version) {
		Map<Integer, Schema> schemaMap = schemas.get(name);
		if (schemaMap != null) {
			schemaMap.remove(version);
		}
	}

	@Override
	public void addSchema(Schema schema) {
		Map<Integer, Schema> schemaMap = schemas.computeIfAbsent(schema.getName(), k -> new HashMap<>());
		if (schemaMap.containsKey(schema.getVersion())) {
			log.error("Schema " + schema.getName() + ", version " + schema.getVersion() + " is already stored.");
			return;
		} else {
			schemaMap.put(schema.getVersion(), schema);
		}
	}

	@Override
	public Microschema getMicroschema(String name) {
		Map<Integer, Microschema> microschemaMap = microschemas.get(name);
		if (microschemaMap == null) {
			return null;
		}
		Optional<Entry<Integer, Microschema>> maxVersion = microschemaMap.entrySet().stream()
				.max((entry1, entry2) -> Integer.compare(entry1.getKey(), entry2.getKey()));
		if (maxVersion.isPresent()) {
			return maxVersion.get().getValue();
		} else {
			return null;
		}
	}

	@Override
	public Microschema getMicroschema(String name, int version) {
		Map<Integer, Microschema> microschemaMap = microschemas.get(name);
		if (microschemaMap == null) {
			return null;
		} else {
			return microschemaMap.get(version);
		}
	}

	@Override
	public void addMicroschema(Microschema microschema) {
		Map<Integer, Microschema> microschemaMap = microschemas.computeIfAbsent(microschema.getName(), k -> new HashMap<>());
		if (microschemaMap.containsKey(microschema.getVersion())) {
			log.error("Microschema " + microschema.getName() + ", version " + microschema.getVersion() + " is already stored.");
			return;
		} else {
			microschemaMap.put(microschema.getVersion(), microschema);
		}
	}

	@Override
	public void removeMicroschema(String name) {
		microschemas.remove(name);
	}

	@Override
	public void removeMicroschema(String name, int version) {
		Map<Integer, Microschema> microschemaMap = microschemas.get(name);
		if (microschemaMap != null) {
			microschemaMap.remove(version);
		}
	}

	/**
	 * Remove the given container from the storage.
	 * 
	 * @param container
	 */
	public void remove(FieldSchemaContainer container) {
		if (container instanceof Schema) {
			removeSchema(container.getName(), container.getVersion());
		} else if (container instanceof Microschema) {
			removeMicroschema(container.getName(), container.getVersion());
		}
	}
}
