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
import com.gentics.mesh.core.data.NodeGraphFieldContainer;
import com.gentics.mesh.core.data.node.Node;
import com.gentics.mesh.core.data.node.field.BinaryGraphField;
import com.gentics.mesh.core.data.node.field.GraphField;
import com.gentics.mesh.core.data.schema.RemoveFieldChange;
import com.gentics.mesh.core.data.schema.SchemaChange;
import com.gentics.mesh.core.data.schema.SchemaContainer;
import com.gentics.mesh.core.data.schema.impl.FieldTypeChangeImpl;
import com.gentics.mesh.core.data.service.ServerSchemaStorage;
import com.gentics.mesh.core.rest.node.NodeResponse;
import com.gentics.mesh.core.rest.node.NodeUpdateRequest;
import com.gentics.mesh.core.rest.node.field.BinaryField;
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
			SchemaChange change = schemaContainer.getNextChange();
			while (change != null) {
				String migrationScript = change.getMigrationScript();
				if (migrationScript != null) {
					migrationScript = migrationScript + "\nnode = JSON.stringify(migrate(JSON.parse(node), fieldname, convert));";
					migrationScripts.add(Tuple.tuple(migrationScript, change.getMigrationScriptContext()));
				}

				// if either the type changes or the field is removed, the field is "touched"
				if (change instanceof FieldTypeChangeImpl) {
					touchedFields.add(((FieldTypeChangeImpl) change).getFieldName());
				} else if (change instanceof RemoveFieldChange) {
					touchedFields.add(((RemoveFieldChange) change).getFieldName());
				}

				change = change.getNextChange();
			}
		} catch (IOException e) {
			return Observable.error(e);
		}

		ScriptEngineManager factory = new ScriptEngineManager();
		for (Node node : nodes) {
			Exception e = db.trx(() -> {
				try {
					for (String languageTag : node.getAvailableLanguageNames()) {
						NodeMigrationActionContextImpl ac = new NodeMigrationActionContextImpl();
						ac.setLanguageTags(Arrays.asList(languageTag));

						NodeResponse restModel = node.transformToRestSync(ac, languageTag).toBlocking().last();
						NodeGraphFieldContainer container = node.getGraphFieldContainer(languageTag);

						// collect the files for all binary fields (keys are the sha512sums, values are filepaths to the binary files)
						Map<String, String> filePaths = container.getFields(schemaContainer.getSchema()).stream()
								.filter(f -> f instanceof BinaryGraphField).map(f -> (BinaryGraphField) f)
								.collect(Collectors.toMap(BinaryGraphField::getSHA512Sum,
										BinaryGraphField::getFilePath, (existingPath, newPath) -> existingPath));

						// remove all touched fields (if necessary, they will be readded later)
						container.getFields(schemaContainer.getSchema()).stream()
								.filter(f -> touchedFields.contains(f.getFieldKey())).forEach(GraphField::removeField);

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
								return new Exception("Transformed node model not found after handling migration scripts");
							}

							nodeJson = transformedNodeModel.toString();
						}

						// transform the result back to the Rest Model
						NodeUpdateRequest updateRequest = JsonUtil.readNode(nodeJson, NodeUpdateRequest.class, ServerSchemaStorage.getSchemaStorage());

						container.updateFieldsFromRest(ac, updateRequest.getFields(), nextVersion.getSchema());

						// create a map containing fieldnames (as keys) and
						// sha512sums of the supposedly stored binary contents
						// of all binary fields
						Map<String, String> existingBinaryFields = nextVersion.getSchema().getFields().stream()
								.filter(f -> "binary".equals(f.getType()))
								.map(f -> Tuple.tuple(f.getName(),
										(BinaryField) updateRequest.getFields().get(f.getName())))
								.filter(t -> t.v2() != null)
								.collect(Collectors.toMap(t -> t.v1(), t -> t.v2().getSha512sum()));

						// check for every binary field in the migrated node,
						// whether the binary file is present, if not, copy it
						// from the old data
						existingBinaryFields.entrySet().stream().forEach(entry -> {
							String fieldName = entry.getKey();
							String sha512Sum = entry.getValue();

							BinaryGraphField binaryField = container.getBinary(fieldName);
							if (binaryField != null && !binaryField.getFile().exists() && filePaths.containsKey(sha512Sum)) {
								Buffer buffer = Buffer.newInstance(Mesh.vertx().fileSystem().readFileBlocking(filePaths.get(sha512Sum)));
								nodeFieldAPIHandler.hashAndStoreBinaryFile(buffer, binaryField.getUuid(), binaryField.getSegmentedPath()).toBlocking().last();
								binaryField.setSHA512Sum(sha512Sum);
							}
						});
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
}
