package com.gentics.mesh.core.data.service;

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;

import javax.annotation.PostConstruct;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import com.gentics.mesh.cli.BootstrapInitializer;
import com.gentics.mesh.core.data.schema.SchemaContainer;
import com.gentics.mesh.core.rest.schema.Microschema;
import com.gentics.mesh.core.rest.schema.Schema;
import com.gentics.mesh.core.rest.schema.SchemaStorage;

import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;

@Component
@Scope(value = "singleton")
public class ServerSchemaStorage implements SchemaStorage {

	private static final Logger log = LoggerFactory.getLogger(ServerSchemaStorage.class);

	public static ServerSchemaStorage instance;

	@PostConstruct
	public void setup() {
		instance = this;
	}

	public static ServerSchemaStorage getSchemaStorage() {
		return instance;
	}

	@Autowired
	private BootstrapInitializer boot;

	/**
	 * Map holding the schemas per name and version
	 */
	private Map<String, Map<Integer, Schema>> schemas = new HashMap<>();

	private Map<String, Microschema> microschemas = new HashMap<>();

	public void init() {
		//Iterate over all schemas and load them into the storage
		for (SchemaContainer container : boot.schemaContainerRoot().findAll()) {
			Schema restSchema = container.getSchema();
			schemas.computeIfAbsent(restSchema.getName(), k -> new HashMap<>()).put(restSchema.getVersion(),
					restSchema);
		}

		// load all microschemas and add to storage
		boot.microschemaContainerRoot().findAll().stream().forEach(container -> addMicroschema(container.getMicroschema()));
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
		return microschemas.get(name);
	}

	@Override
	public void addMicroschema(Microschema microschema) {
		if (microschemas.containsKey(microschema.getName())) {
			log.error("Microschema " + microschema.getName() + " is already stored.");
			return;
		} else {
			microschemas.put(microschema.getName(), microschema);
		}
	}

	@Override
	public void removeMicroschema(String name) {
		microschemas.remove(name);
	}
}
