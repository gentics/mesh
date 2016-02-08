package com.gentics.mesh.core.data.node.handler;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;

import org.springframework.stereotype.Component;

import com.gentics.mesh.core.data.NodeGraphFieldContainer;
import com.gentics.mesh.core.data.node.Node;
import com.gentics.mesh.core.data.node.field.GraphField;
import com.gentics.mesh.core.data.schema.SchemaChange;
import com.gentics.mesh.core.data.schema.SchemaContainer;
import com.gentics.mesh.core.data.service.ServerSchemaStorage;
import com.gentics.mesh.core.rest.node.NodeResponse;
import com.gentics.mesh.core.rest.node.NodeUpdateRequest;
import com.gentics.mesh.core.verticle.handler.AbstractHandler;
import com.gentics.mesh.graphdb.NoTrx;
import com.gentics.mesh.handler.impl.NodeMigrationActionContextImpl;
import com.gentics.mesh.json.JsonUtil;
import com.gentics.mesh.util.Tuple;

/**
 * Handler for node migrations after schema updates
 */
@Component
public class NodeMigrationHandler extends AbstractHandler {
	/**
	 * Migrate all nodes referencing the given schema container to the latest
	 * version of the schema
	 *
	 * @param schemaContainer
	 *            schema container
	 */
	public void migrateNodes(SchemaContainer schemaContainer) throws IOException {
		// get the next schema container (if any)
		SchemaContainer nextVersion = db.noTrx(schemaContainer::getNextVersion);

		// no next version, migration is done
		if (nextVersion == null) {
			return;
		}

		// get the nodes, that need to be transformed
		List<? extends Node> nodes = db.noTrx(schemaContainer::getNodes);

		// no nodes, migration is done
		if (nodes.isEmpty()) {
			return;
		}

		// collect the migration scripts
		List<Tuple<String, List<Tuple<String, Object>>>> migrationScripts = new ArrayList<>();
		try (NoTrx noTrx = db.noTrx()) {
			SchemaChange change = schemaContainer.getNextChange();
			while (change != null) {
				String migrationScript = change.getMigrationScript();
				if (migrationScript != null) {
					migrationScripts.add(Tuple.tuple(migrationScript, change.getMigrationScriptContext()));
				}
				change = change.getNextChange();
			}
		}

		ScriptEngineManager factory = new ScriptEngineManager();
		for (Node node : nodes) {
			db.trx(() -> {
				for (String languageTag : node.getAvailableLanguageNames()) {
					NodeMigrationActionContextImpl ac = new NodeMigrationActionContextImpl();
					ac.setLanguageTags(Arrays.asList(languageTag));

					NodeResponse restModel = node.transformToRest(ac, languageTag).toBlocking().last();
					NodeGraphFieldContainer container = node.getGraphFieldContainer(languageTag);

					// remove all existing fields
					container.getFields(schemaContainer.getSchema()).stream().forEach(GraphField::removeField);

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
							// TODO fail
						}

						nodeJson = transformedNodeModel.toString();
					}

					// transform the result back to the Rest Model
					NodeUpdateRequest updateRequest = JsonUtil.readNode(nodeJson, NodeUpdateRequest.class, ServerSchemaStorage.getSchemaStorage());

					container.updateFieldsFromRest(ac, updateRequest.getFields(), nextVersion.getSchema());
				}
				// migrate the schema reference to the new version
				node.setSchemaContainer(nextVersion);

				return null;
			});
		}
	}
}
