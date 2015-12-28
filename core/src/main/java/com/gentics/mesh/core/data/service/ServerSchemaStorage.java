package com.gentics.mesh.core.data.service;

import java.util.HashMap;
import java.util.Map;

import javax.annotation.PostConstruct;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import com.gentics.mesh.cli.BootstrapInitializer;
import com.gentics.mesh.core.data.SchemaContainer;
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

	private Map<String, Schema> schemas = new HashMap<>();

	private Map<String, Microschema> microschemas = new HashMap<>();

	public void init() {
		//Iterate over all schemas and load them into the storage
		for (SchemaContainer container : boot.schemaContainerRoot().findAll()) {
			Schema restSchema = container.getSchema();
			schemas.put(restSchema.getName(), restSchema);
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
		return schemas.get(name);
	}

	@Override
	public void removeSchema(String name) {
		schemas.remove(name);
	}

	@Override
	public void addSchema(Schema schema) {
		if (schemas.containsKey(schema.getName())) {
			log.error("Schema " + schema.getName() + " is already stored.");
			return;
		} else {
			schemas.put(schema.getName(), schema);
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
