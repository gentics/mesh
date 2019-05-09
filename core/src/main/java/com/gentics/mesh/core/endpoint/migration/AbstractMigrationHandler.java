package com.gentics.mesh.core.endpoint.migration;

import com.gentics.mesh.context.impl.NodeMigrationActionContextImpl;
import com.gentics.mesh.core.data.GraphFieldContainer;
import com.gentics.mesh.core.data.schema.GraphFieldSchemaContainerVersion;
import com.gentics.mesh.core.data.schema.RemoveFieldChange;
import com.gentics.mesh.core.data.schema.SchemaChange;
import com.gentics.mesh.core.data.schema.impl.FieldTypeChangeImpl;
import com.gentics.mesh.core.data.search.SearchQueue;
import com.gentics.mesh.core.data.search.SearchQueueBatch;
import com.gentics.mesh.core.endpoint.handler.AbstractHandler;
import com.gentics.mesh.core.endpoint.node.BinaryFieldHandler;
import com.gentics.mesh.core.rest.common.FieldContainer;
import com.gentics.mesh.core.rest.node.FieldMap;
import com.gentics.mesh.core.rest.node.field.Field;
import com.gentics.mesh.graphdb.spi.Database;
import com.gentics.mesh.metric.MetricsService;
import com.gentics.mesh.util.StreamUtil;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import jdk.nashorn.api.scripting.ClassFilter;
import jdk.nashorn.api.scripting.NashornScriptEngineFactory;

import javax.annotation.ParametersAreNonnullByDefault;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

@SuppressWarnings("restriction")
public abstract class AbstractMigrationHandler extends AbstractHandler implements MigrationHandler {

	private static final Logger log = LoggerFactory.getLogger(AbstractMigrationHandler.class);

	/**
	 * Script engine factory.
	 */
	protected NashornScriptEngineFactory factory = new NashornScriptEngineFactory();

	protected Database db;

	protected SearchQueue searchQueue;

	protected BinaryFieldHandler binaryFieldHandler;

	protected MetricsService metrics;

	public AbstractMigrationHandler(Database db, SearchQueue searchQueue, BinaryFieldHandler binaryFieldHandler, MetricsService metrics) {
		this.db = db;
		this.searchQueue = searchQueue;
		this.binaryFieldHandler = binaryFieldHandler;
		this.metrics = metrics;
	}

	/**
	 * Collect the migration scripts and set of touched fields when migrating the given container into the next version
	 *
	 * @param fromVersion
	 *            Container which contains the expected migration changes
	 * @param touchedFields
	 *            Set of touched fields (will be modified)
	 * @throws IOException
	 */
	protected void prepareMigration(GraphFieldSchemaContainerVersion<?, ?, ?, ?, ?> fromVersion, Set<String> touchedFields) throws IOException {
		SchemaChange<?> change = fromVersion.getNextChange();
		while (change != null) {
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
	 * @param fromVersion
	 *            rest model of the container
	 * @param newVersion
	 *            new schema version
	 * @param touchedFields
	 *            set of touched fields
	 * @throws Exception
	 */
	protected void migrate(NodeMigrationActionContextImpl ac, GraphFieldContainer newContainer, FieldContainer newContent,
		   	GraphFieldSchemaContainerVersion<?, ?, ?, ?, ?> fromVersion,
			GraphFieldSchemaContainerVersion<?, ?, ?, ?, ?> newVersion, Set<String> touchedFields) throws Exception {

		// Remove all touched fields (if necessary, they will be readded later)
		newContainer.getFields().stream().filter(f -> touchedFields.contains(f.getFieldKey())).forEach(f -> f.removeField(newContainer));
		newContainer.setSchemaContainerVersion(newVersion);

		FieldMap fields = newContent.getFields();

		Map<String, Field> newFields = fromVersion.getChanges()
			.map(change -> change.createFields(fromVersion.getSchema(), newContent))
			.collect(StreamUtil.mergeMaps());

		fields.clear();
		fields.putAll(newFields);

		newContainer.updateFieldsFromRest(ac, fields);
	}

	@ParametersAreNonnullByDefault
	protected <T> List<Exception> migrateLoop(Iterable<T> containers, MigrationStatusHandler status, TriConsumer<SearchQueueBatch, T, List<Exception>> migrator) {
		// Iterate over all containers and invoke a migration for each one
		long count = 0;
		List<Exception> errorsDetected = new ArrayList<>();
		SearchQueueBatch sqb = searchQueue.create();
		for (T container : containers) {
			try {
				// Each container migration has its own search queue batch which is then combined with other batch entries.
				// This prevents adding partial entries from failed migrations.
				SearchQueueBatch containerBatch = searchQueue.create();
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
					sqb.processSync();
					sqb.clear();
				});
			}
		}
		if (sqb.size() > 0) {
			log.info("Syncing last batch with size: " + sqb.size());
			db.tx(() -> {
				sqb.processSync();
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
