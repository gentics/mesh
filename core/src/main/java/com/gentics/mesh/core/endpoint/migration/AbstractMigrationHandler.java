package com.gentics.mesh.core.endpoint.migration;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import javax.annotation.ParametersAreNonnullByDefault;
import javax.script.ScriptEngine;

import com.gentics.mesh.context.impl.NodeMigrationActionContextImpl;
import com.gentics.mesh.core.data.GraphFieldContainer;
import com.gentics.mesh.core.data.node.handler.TypeConverter;
import com.gentics.mesh.core.data.schema.GraphFieldSchemaContainerVersion;
import com.gentics.mesh.core.data.schema.RemoveFieldChange;
import com.gentics.mesh.core.data.schema.SchemaChange;
import com.gentics.mesh.core.data.schema.impl.FieldTypeChangeImpl;
import com.gentics.mesh.core.endpoint.handler.AbstractHandler;
import com.gentics.mesh.core.endpoint.node.BinaryUploadHandler;
import com.gentics.mesh.core.rest.common.FieldContainer;
import com.gentics.mesh.core.rest.common.RestModel;
import com.gentics.mesh.core.rest.event.EventCauseInfo;
import com.gentics.mesh.event.EventQueueBatch;
import com.gentics.mesh.graphdb.spi.Database;
import com.gentics.mesh.json.JsonUtil;
import com.gentics.mesh.metric.MetricsService;
import com.gentics.mesh.util.Tuple;

import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import jdk.nashorn.api.scripting.ClassFilter;
import jdk.nashorn.api.scripting.NashornScriptEngineFactory;

@SuppressWarnings("restriction")
public abstract class AbstractMigrationHandler extends AbstractHandler implements MigrationHandler {

	private static final Logger log = LoggerFactory.getLogger(AbstractMigrationHandler.class);

	/**
	 * Script engine factory.
	 */
	protected NashornScriptEngineFactory factory = new NashornScriptEngineFactory();

	protected Database db;

	protected BinaryUploadHandler binaryFieldHandler;

	protected MetricsService metrics;

	public AbstractMigrationHandler(Database db, BinaryUploadHandler binaryFieldHandler, MetricsService metrics) {
		this.db = db;
		this.binaryFieldHandler = binaryFieldHandler;
		this.metrics = metrics;
	}

	/**
	 * Collect the migration scripts and set of touched fields when migrating the given container into the next version
	 *
	 * @param fromVersion
	 *            Container which contains the expected migration changes
	 * @param migrationScripts
	 *            List of migration scripts (will be modified)
	 * @param touchedFields
	 *            Set of touched fields (will be modified)
	 * @throws IOException
	 */
	protected void prepareMigration(GraphFieldSchemaContainerVersion<?, ?, ?, ?, ?> fromVersion,
			List<Tuple<String, List<Tuple<String, Object>>>> migrationScripts, Set<String> touchedFields) throws IOException {
		SchemaChange<?> change = fromVersion.getNextChange();
		while (change != null) {
			String migrationScript = change.getMigrationScript();
			if (migrationScript != null) {
				migrationScript = migrationScript + "\nnode = JSON.stringify(migrate(JSON.parse(node), fieldname, convert));";
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

	/**
	 * Migrate the given container. This will also set the new version to the container.
	 * 
	 * @param ac
	 *            context
	 * @param container
	 *            container to migrate
	 * @param restModel
	 *            rest model of the container
	 * @param newVersion
	 *            new schema version
	 * @param touchedFields
	 *            set of touched fields
	 * @param migrationScripts
	 *            list of migration scripts
	 * @param clazz
	 * @throws Exception
	 */
	protected <T extends FieldContainer> void migrate(NodeMigrationActionContextImpl ac, GraphFieldContainer container, RestModel restModel,
			GraphFieldSchemaContainerVersion<?, ?, ?, ?, ?> newVersion, Set<String> touchedFields,
			List<Tuple<String, List<Tuple<String, Object>>>> migrationScripts, Class<T> clazz) throws Exception {

		// Remove all touched fields (if necessary, they will be readded later)
		container.getFields().stream().filter(f -> touchedFields.contains(f.getFieldKey())).forEach(f -> f.removeField(container));

		String nodeJson = restModel.toJson();

		for (Tuple<String, List<Tuple<String, Object>>> scriptEntry : migrationScripts) {
			String script = scriptEntry.v1();
			List<Tuple<String, Object>> context = scriptEntry.v2();
			ScriptEngine engine = factory.getScriptEngine(new Sandbox());

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

		// Transform the result back to the Rest Model
		T transformedRestModel = JsonUtil.readValue(nodeJson, clazz);

		container.setSchemaContainerVersion(newVersion);
		container.updateFieldsFromRest(ac, transformedRestModel.getFields());

	}

	@ParametersAreNonnullByDefault
	protected <T> List<Exception> migrateLoop(Iterable<T> containers, EventCauseInfo cause, MigrationStatusHandler status, TriConsumer<EventQueueBatch, T, List<Exception>> migrator) {
		// Iterate over all containers and invoke a migration for each one
		long count = 0;
		List<Exception> errorsDetected = new ArrayList<>();
		EventQueueBatch sqb = EventQueueBatch.create();
		sqb.setCause(cause);
		for (T container : containers) {
			try {
				// Each container migration has its own search queue batch which is then combined with other batch entries.
				// This prevents adding partial entries from failed migrations.
				EventQueueBatch containerBatch = EventQueueBatch.create();
				db.tx(() -> {
					migrator.accept(containerBatch, container, errorsDetected);
				});
				sqb.addAll(containerBatch);
				status.incCompleted();
				if (count % 50 == 0) {
					log.info("Migrated containers: " + count);
				}
				count++;
			} catch (Exception e) {
				errorsDetected.add(e);
			}

			if (count % 500 == 0) {
				// Process the batch and reset it
				log.info("Syncing batch with size: " + sqb.size());
				db.tx(() -> {
					sqb.dispatch();
					sqb.clear();
				});
			}
		}
		if (sqb.size() > 0) {
			log.info("Syncing last batch with size: " + sqb.size());
			db.tx(() -> {
				sqb.dispatch();
			});
		}

		log.info("Migration of " + count + " containers done..");
		log.info("Encountered {" + errorsDetected.size() + "} errors during node migration.");
		return errorsDetected;
	}

	/**
	 * Sandbox classfilter that filters all classes
	 */
	protected static class Sandbox implements ClassFilter {
		@Override
		public boolean exposeToScripts(String className) {
			return false;
		}
	}

}
