package com.gentics.mesh.core.data.service;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;

import javax.inject.Inject;
import javax.inject.Singleton;

import com.gentics.mesh.cli.BootstrapInitializer;
import com.gentics.mesh.core.data.dao.MicroschemaDaoWrapper;
import com.gentics.mesh.core.data.dao.SchemaDaoWrapper;
import com.gentics.mesh.core.data.schema.Microschema;
import com.gentics.mesh.core.data.schema.MicroschemaVersion;
import com.gentics.mesh.core.data.schema.Schema;
import com.gentics.mesh.core.data.schema.SchemaVersion;
import com.gentics.mesh.core.db.Tx;
import com.gentics.mesh.core.rest.microschema.MicroschemaVersionModel;
import com.gentics.mesh.core.rest.schema.FieldSchemaContainer;
import com.gentics.mesh.core.rest.schema.SchemaStorage;
import com.gentics.mesh.core.rest.schema.SchemaUpdateModel;

import dagger.Lazy;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;

/**
 * Central schema storage system which is used to buffer/cache JSON schema data. Storing the schema pojo's in memory is not expensive and help peformance a lot
 * since it is not required to load the schema from the graph everytime it is needed.
 */
@Singleton
public class ServerSchemaStorage implements SchemaStorage {

	private static final Logger log = LoggerFactory.getLogger(ServerSchemaStorage.class);

	private Lazy<BootstrapInitializer> boot;

	/**
	 * Map holding the schemas per name and version
	 */
	private Map<String, Map<String, SchemaUpdateModel>> schemas = Collections.synchronizedMap(new HashMap<>());

	private Map<String, Map<String, MicroschemaVersionModel>> microschemas = Collections.synchronizedMap(new HashMap<>());

	@Inject
	public ServerSchemaStorage(Lazy<BootstrapInitializer> boot) {
		this.boot = boot;
	}

	public void init() {
		// Iterate over all schemas and load them into the storage
		SchemaDaoWrapper schemaDao = Tx.get().data().schemaDao();
		MicroschemaDaoWrapper microschemaDao = Tx.get().data().microschemaDao();

		for (Schema container : schemaDao.findAll()) {
			for (SchemaVersion version : container.findAll()) {
				SchemaUpdateModel restSchema = version.getSchema();
				schemas.computeIfAbsent(restSchema.getName(), k -> new HashMap<>()).put(restSchema.getVersion(), restSchema);
			}
		}

		// load all microschemas and add to storage
		for (Microschema container : microschemaDao.findAll()) {
			for (MicroschemaVersion version : container.findAll()) {
				MicroschemaVersionModel restMicroschema = version.getSchema();
				microschemas.computeIfAbsent(restMicroschema.getName(), k -> new HashMap<>()).put(restMicroschema.getVersion(), restMicroschema);
			}
		}
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
	public SchemaUpdateModel getSchema(String name) {
		Map<String, SchemaUpdateModel> schemaMap = schemas.get(name);
		if (schemaMap == null) {
			return null;
		}
		Optional<Entry<String, SchemaUpdateModel>> maxVersion = schemaMap.entrySet().stream().max((entry1, entry2) -> {
			Double v1 = Double.valueOf(entry1.getKey());
			Double v2 = Double.valueOf(entry2.getKey());
			return Double.compare(v1, v2);
		});
		if (maxVersion.isPresent()) {
			return maxVersion.get().getValue();
		} else {
			return null;
		}
	}

	@Override
	public SchemaUpdateModel getSchema(String name, String version) {
		Map<String, SchemaUpdateModel> schemaMap = schemas.get(name);
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
	public void removeSchema(String name, String version) {
		Map<String, SchemaUpdateModel> schemaMap = schemas.get(name);
		if (schemaMap != null) {
			schemaMap.remove(version);
		}
	}

	@Override
	public void addSchema(SchemaUpdateModel schema) {
		Map<String, SchemaUpdateModel> schemaMap = schemas.computeIfAbsent(schema.getName(), k -> new HashMap<>());
		if (schemaMap.containsKey(schema.getVersion())) {
			log.error("Schema " + schema.getName() + ", version " + schema.getVersion() + " is already stored.");
			return;
		} else {
			schemaMap.put(schema.getVersion(), schema);
		}
	}

	@Override
	public MicroschemaVersionModel getMicroschema(String name) {
		Map<String, MicroschemaVersionModel> microschemaMap = microschemas.get(name);
		if (microschemaMap == null) {
			return null;
		}
		Optional<Entry<String, MicroschemaVersionModel>> maxVersion = microschemaMap.entrySet().stream().max((entry1, entry2) -> {
			Double v1 = Double.valueOf(entry1.getKey());
			Double v2 = Double.valueOf(entry2.getKey());
			return Double.compare(v1, v2);
		});
		if (maxVersion.isPresent()) {
			return maxVersion.get().getValue();
		} else {
			return null;
		}
	}

	@Override
	public MicroschemaVersionModel getMicroschema(String name, String version) {
		Map<String, MicroschemaVersionModel> microschemaMap = microschemas.get(name);
		if (microschemaMap == null) {
			return null;
		} else {
			return microschemaMap.get(version);
		}
	}

	@Override
	public void addMicroschema(MicroschemaVersionModel microschema) {
		Map<String, MicroschemaVersionModel> microschemaMap = microschemas.computeIfAbsent(microschema.getName(), k -> new HashMap<>());
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
	public void removeMicroschema(String name, String version) {
		Map<String, MicroschemaVersionModel> microschemaMap = microschemas.get(name);
		if (microschemaMap != null) {
			microschemaMap.remove(version);
		}
	}

	/**
	 * Remove the given container from the storage.
	 * 
	 * @param container
	 *            Schema or microschema container which is used to identify the elements which should be removed from the storage
	 */
	public void remove(FieldSchemaContainer container) {
		if (container instanceof SchemaUpdateModel) {
			SchemaUpdateModel schemaModel = (SchemaUpdateModel) container;
			removeSchema(schemaModel.getName(), schemaModel.getVersion());
		} else if (container instanceof MicroschemaVersionModel) {
			MicroschemaVersionModel schemaModel = (MicroschemaVersionModel) container;
			removeMicroschema(schemaModel.getName(), schemaModel.getVersion());
		}
	}
}
