package com.gentics.mesh.core.data.node.handler;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.gentics.mesh.Mesh;
import com.gentics.mesh.core.data.GraphFieldContainer;
import com.gentics.mesh.core.data.MicroschemaContainer;
import com.gentics.mesh.core.data.NodeGraphFieldContainer;
import com.gentics.mesh.core.data.node.Micronode;
import com.gentics.mesh.core.data.node.Node;
import com.gentics.mesh.core.data.node.field.BinaryGraphField;
import com.gentics.mesh.core.data.node.field.GraphField;
import com.gentics.mesh.core.data.schema.GraphFieldSchemaContainer;
import com.gentics.mesh.core.data.schema.RemoveFieldChange;
import com.gentics.mesh.core.data.schema.SchemaChange;
import com.gentics.mesh.core.data.schema.SchemaContainer;
import com.gentics.mesh.core.data.schema.impl.FieldTypeChangeImpl;
import com.gentics.mesh.core.data.service.ServerSchemaStorage;
import com.gentics.mesh.core.rest.common.FieldContainer;
import com.gentics.mesh.core.rest.common.RestModel;
import com.gentics.mesh.core.rest.micronode.MicronodeResponse;
import com.gentics.mesh.core.rest.node.NodeResponse;
import com.gentics.mesh.core.rest.node.NodeUpdateRequest;
import com.gentics.mesh.core.rest.node.field.BinaryField;
import com.gentics.mesh.core.rest.schema.FieldSchemaContainer;
import com.gentics.mesh.core.rest.schema.Microschema;
import com.gentics.mesh.core.rest.schema.Schema;
import com.gentics.mesh.core.verticle.handler.AbstractHandler;
import com.gentics.mesh.core.verticle.node.NodeFieldAPIHandler;
import com.gentics.mesh.core.verticle.node.NodeMigrationStatus;
import com.gentics.mesh.graphdb.NoTrx;
import com.gentics.mesh.handler.impl.NodeMigrationActionContextImpl;
import com.gentics.mesh.json.JsonUtil;
import com.gentics.mesh.util.Tuple;

import io.vertx.rxjava.core.buffer.Buffer;
import rx.Observable;

/**
 * Handler for node migrations after schema updates
 */
@Component
public class NodeMigrationHandler extends AbstractHandler {
	@Autowired
	private NodeFieldAPIHandler nodeFieldAPIHandler;

	/**
	 * Script engine factory
	 */
	private ScriptEngineManager factory = new ScriptEngineManager();

	/**
	 * Migrate all nodes referencing the given schema container to the latest
	 * version of the schema
	 *
	 * @param schemaContainer
	 *            schema container
	 * @param statusMBean status MBean
	 */
	public Observable<Void> migrateNodes(SchemaContainer schemaContainer, NodeMigrationStatus statusMBean) {
		if (schemaContainer == null) {
			return Observable.error(new Exception("Cannot start node migration without schema"));
		}
		// get the next schema container (if any)
		SchemaContainer nextVersion = db.noTrx(schemaContainer::getNextVersion);

		// no next version, migration is done
		if (nextVersion == null) {
			return Observable.just(null);
		}

		// get the nodes, that need to be transformed
		List<? extends Node> nodes = db.noTrx(schemaContainer::getNodes);

		// no nodes, migration is done
		if (nodes.isEmpty()) {
			return Observable.just(null);
		}

		if (statusMBean != null) {
			statusMBean.setTotalNodes(nodes.size());
		}

		// collect the migration scripts
		List<Tuple<String, List<Tuple<String, Object>>>> migrationScripts = new ArrayList<>();
		Set<String> touchedFields = new HashSet<>();
		try (NoTrx noTrx = db.noTrx()) {
			prepareMigration(schemaContainer, migrationScripts, touchedFields);
		} catch (IOException e) {
			return Observable.error(e);
		}

		for (Node node : nodes) {
			Exception e = db.trx(() -> {
				try {
					for (String languageTag : node.getAvailableLanguageNames()) {
						NodeMigrationActionContextImpl ac = new NodeMigrationActionContextImpl();
						ac.setLanguageTags(Arrays.asList(languageTag));

						NodeResponse restModel = node.transformToRestSync(ac, languageTag).toBlocking().last();
						NodeGraphFieldContainer container = node.getGraphFieldContainer(languageTag);
						Schema oldSchema = schemaContainer.getSchema();
						Schema newSchema = nextVersion.getSchema();
						migrate(ac, container, restModel, oldSchema, newSchema, touchedFields, migrationScripts, NodeUpdateRequest.class);
					}
					// migrate the schema reference to the new version
					node.setSchemaContainer(nextVersion);

					return null;
				} catch (Exception e1) {
					return e1;
				}
			});

			if (e != null) {
				return Observable.error(e);
			}

			if (statusMBean != null) {
				statusMBean.incNodesDone();
			}
		}

		return Observable.just(null);
	}

	/**
	 * Migrate all micronodes referencing the given microschema container to the latest version
	 *
	 * @param microschemaContainer microschema container
	 * @param statusMBean JMX Status bean
	 * @return
	 */
	public Observable<Void> migrateMicronodes(MicroschemaContainer microschemaContainer, NodeMigrationStatus statusMBean) {
		if (microschemaContainer == null) {
			return Observable.error(new Exception("Cannot start micronode migration without microschema"));
		}
		// get the next schema container (if any)
		MicroschemaContainer nextVersion = db.noTrx(microschemaContainer::getNextVersion);

		// no next version, migration is done
		if (nextVersion == null) {
			return Observable.just(null);
		}

		// get the nodes, that need to be transformed
		// TODO get micronodes
		List<? extends Micronode> micronodes = db.noTrx(microschemaContainer::getMicronodes);

		// no micronodes, migration is done
		if (micronodes.isEmpty()) {
			return Observable.just(null);
		}

		if (statusMBean != null) {
			statusMBean.setTotalNodes(micronodes.size());
		}

		// collect the migration scripts
		List<Tuple<String, List<Tuple<String, Object>>>> migrationScripts = new ArrayList<>();
		Set<String> touchedFields = new HashSet<>();
		try (NoTrx noTrx = db.noTrx()) {
			prepareMigration(microschemaContainer, migrationScripts, touchedFields);
		} catch (IOException e) {
			return Observable.error(e);
		}

		for (Micronode micronode : micronodes) {
			Exception e = db.trx(() -> {
				try {
					NodeMigrationActionContextImpl ac = new NodeMigrationActionContextImpl();
					NodeGraphFieldContainer container = micronode.getContainer();
					if (container == null) {
						throw new Exception("Found micronode without container");
					}
					ac.setLanguageTags(Arrays.asList(container.getLanguage().getLanguageTag()));

					MicronodeResponse restModel = micronode.transformToRestSync(ac).toBlocking().last();
					Microschema oldSchema = microschemaContainer.getSchema();
					Microschema newSchema = nextVersion.getSchema();
					migrate(ac, micronode, restModel, oldSchema, newSchema, touchedFields, migrationScripts, MicronodeResponse.class);
					// migrate the microschema reference to the new version
					micronode.setMicroschemaContainer(nextVersion);

					return null;
				} catch (Exception e1) {
					return e1;
				}
			});

			if (e != null) {
				return Observable.error(e);
			}

			if (statusMBean != null) {
				statusMBean.incNodesDone();
			}
		}

		return Observable.just(null);
	}

	/**
	 * Migrate the given container
	 * 
	 * @param ac context
	 * @param container container to migrate
	 * @param restModel rest model of the container
	 * @param oldSchema old schema
	 * @param newSchema new schema
	 * @param touchedFields set of touched fields
	 * @param migrationScripts list of migration scripts
	 * @param clazz
	 * @throws Exception
	 */
	protected <T extends FieldContainer> void migrate(NodeMigrationActionContextImpl ac, GraphFieldContainer container, RestModel restModel,
			FieldSchemaContainer oldSchema, FieldSchemaContainer newSchema, Set<String> touchedFields,
			List<Tuple<String, List<Tuple<String, Object>>>> migrationScripts, Class<T> clazz) throws Exception {
		// collect the files for all binary fields (keys are the sha512sums,
		// values are filepaths to the binary files)
		Map<String, String> filePaths = container.getFields(oldSchema).stream()
				.filter(f -> f instanceof BinaryGraphField).map(f -> (BinaryGraphField) f)
				.collect(Collectors.toMap(BinaryGraphField::getSHA512Sum, BinaryGraphField::getFilePath,
						(existingPath, newPath) -> existingPath));

		// remove all touched fields (if necessary, they will be readded later)
		container.getFields(oldSchema).stream().filter(f -> touchedFields.contains(f.getFieldKey()))
				.forEach(GraphField::removeField);

		String nodeJson = JsonUtil.toJson(restModel);

		for (Tuple<String, List<Tuple<String, Object>>> scriptEntry : migrationScripts) {
			String script = scriptEntry.v1();
			List<Tuple<String, Object>> context = scriptEntry.v2();
			ScriptEngine engine = factory.getEngineByName("JavaScript");

			engine.put("node", nodeJson);
			engine.put("convert", new TypeConverter());
			if (context != null) {
				for (Tuple<String, Object> ctxEntry : context) {
					engine.put(ctxEntry.v1(), ctxEntry.v2());
				}
			}
			engine.eval(script);

			Object transformedNodeModel = engine.get("node");

			if (transformedNodeModel == null) {
				throw new Exception("Transformed node model not found after handling migration scripts");
			}

			nodeJson = transformedNodeModel.toString();
		}

		// transform the result back to the Rest Model
		T transformedRestModel = JsonUtil.readNode(nodeJson, clazz, ServerSchemaStorage.getSchemaStorage());

		container.updateFieldsFromRest(ac, transformedRestModel.getFields(), newSchema);

		// create a map containing fieldnames (as keys) and
		// sha512sums of the supposedly stored binary contents
		// of all binary fields
		Map<String, String> existingBinaryFields = newSchema.getFields().stream()
				.filter(f -> "binary".equals(f.getType()))
				.map(f -> Tuple.tuple(f.getName(), (BinaryField) transformedRestModel.getFields().get(f.getName())))
				.filter(t -> t.v2() != null).collect(Collectors.toMap(t -> t.v1(), t -> t.v2().getSha512sum()));

		// check for every binary field in the migrated node,
		// whether the binary file is present, if not, copy it
		// from the old data
		existingBinaryFields.entrySet().stream().forEach(entry -> {
			String fieldName = entry.getKey();
			String sha512Sum = entry.getValue();

			BinaryGraphField binaryField = container.getBinary(fieldName);
			if (binaryField != null && !binaryField.getFile().exists() && filePaths.containsKey(sha512Sum)) {
				Buffer buffer = Buffer
						.newInstance(Mesh.vertx().fileSystem().readFileBlocking(filePaths.get(sha512Sum)));
				nodeFieldAPIHandler
						.hashAndStoreBinaryFile(buffer, binaryField.getUuid(), binaryField.getSegmentedPath())
						.toBlocking().last();
				binaryField.setSHA512Sum(sha512Sum);
			}
		});
	}

	/**
	 * Collect the migration scripts and set of touched fields when migrating the given container into the next version
	 *
	 * @param container container
	 * @param migrationScripts list of migration scripts (will be modified)
	 * @param touchedFields set of touched fields (will be modified)
	 * @throws IOException
	 */
	protected void prepareMigration(GraphFieldSchemaContainer<?, ?, ?> container,
			List<Tuple<String, List<Tuple<String, Object>>>> migrationScripts, Set<String> touchedFields)
					throws IOException {
		SchemaChange<?> change = container.getNextChange();
		while (change != null) {
			String migrationScript = change.getMigrationScript();
			if (migrationScript != null) {
				migrationScript = migrationScript
						+ "\nnode = JSON.stringify(migrate(JSON.parse(node), fieldname, convert));";
				migrationScripts.add(Tuple.tuple(migrationScript, change.getMigrationScriptContext()));
			}

			// if either the type changes or the field is removed, the field is
			// "touched"
			if (change instanceof FieldTypeChangeImpl) {
				touchedFields.add(((FieldTypeChangeImpl) change).getFieldName());
			} else if (change instanceof RemoveFieldChange) {
				touchedFields.add(((RemoveFieldChange) change).getFieldName());
			}

			change = change.getNextChange();
		}
	}
}
