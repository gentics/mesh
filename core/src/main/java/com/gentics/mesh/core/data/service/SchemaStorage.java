package com.gentics.mesh.core.data.service;

import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import javax.annotation.PostConstruct;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import com.gentics.mesh.cli.BootstrapInitializer;
import com.gentics.mesh.core.data.SchemaContainer;
import com.gentics.mesh.core.rest.schema.Schema;

@Component
@Scope(value = "singleton")
public class SchemaStorage {

	private static final Logger log = LoggerFactory.getLogger(SchemaStorage.class);

	public static SchemaStorage instance;

	@PostConstruct
	public void setup() {
		instance = this;
	}

	public static SchemaStorage getSchemaStorage() {
		return instance;
	}

	@Autowired
	private BootstrapInitializer boot;

	private Map<String, Schema> schemas = new HashMap<>();

	public void init() {
		//Iterate over all schemas and load them into the storage
		for (SchemaContainer container : boot.schemaContainerRoot().findAll()) {
			try {
				Schema restSchema = container.getSchema();
				schemas.put(restSchema.getName(), restSchema);
			} catch (IOException e) {
				log.error("Could not load schema with uuid {" + container.getUuid() + "}", e);
				e.printStackTrace();
			}
		}
	}

	public void clear() {
		schemas.clear();
	}

	public int size() {
		return schemas.size();
	}

	public Schema getSchema(String name) {
		return schemas.get(name);
	}

	public void removeSchema(String name) {
		schemas.remove(name);
	}

	public void addSchema(Schema schema) {
		if (schemas.containsKey(schema.getName())) {
			log.error("Schema " + schema.getName() + " is already stored.");
			return;
		} else {
			schemas.put(schema.getName(), schema);
		}
	}

}
