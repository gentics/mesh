package com.gentics.mesh.core.migration;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.annotation.ParametersAreNonnullByDefault;
import javax.inject.Provider;

import com.gentics.mesh.context.NodeMigrationActionContext;
import com.gentics.mesh.core.data.GraphFieldContainer;
import com.gentics.mesh.core.data.NodeGraphFieldContainer;
import com.gentics.mesh.core.data.dao.ContentDaoWrapper;
import com.gentics.mesh.core.data.schema.GraphFieldSchemaContainerVersion;
import com.gentics.mesh.core.data.schema.HibFieldSchemaVersionElement;
import com.gentics.mesh.core.data.schema.RemoveFieldChange;
import com.gentics.mesh.core.data.schema.SchemaChange;
import com.gentics.mesh.core.data.schema.impl.FieldTypeChangeImpl;
import com.gentics.mesh.core.db.Tx;
import com.gentics.mesh.core.endpoint.handler.AbstractHandler;
import com.gentics.mesh.core.endpoint.migration.MigrationHandler;
import com.gentics.mesh.core.endpoint.migration.MigrationStatusHandler;
import com.gentics.mesh.core.endpoint.migration.TriConsumer;
import com.gentics.mesh.core.endpoint.node.BinaryUploadHandler;
import com.gentics.mesh.core.rest.common.FieldContainer;
import com.gentics.mesh.core.rest.event.EventCauseInfo;
import com.gentics.mesh.core.rest.node.FieldMap;
import com.gentics.mesh.core.rest.node.field.Field;
import com.gentics.mesh.event.EventQueueBatch;
import com.gentics.mesh.graphdb.spi.Database;
import com.gentics.mesh.metric.MetricsService;
import com.gentics.mesh.util.StreamUtil;

import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;

public abstract class AbstractMigrationHandler extends AbstractHandler implements MigrationHandler {

	private static final Logger log = LoggerFactory.getLogger(AbstractMigrationHandler.class);

	protected Database db;

	protected BinaryUploadHandler binaryFieldHandler;

	protected MetricsService metrics;

	protected final Provider<EventQueueBatch> batchProvider;

	public AbstractMigrationHandler(Database db, BinaryUploadHandler binaryFieldHandler, MetricsService metrics, Provider<EventQueueBatch> batchProvider) {
		this.db = db;
		this.binaryFieldHandler = binaryFieldHandler;
		this.metrics = metrics;
		this.batchProvider = batchProvider;
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
//			if (change instanceof UpdateFieldChangeImpl) {
//				touchedFields.add(((UpdateFieldChangeImpl) change).getFieldName());
//			} else
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
	protected void migrate(NodeMigrationActionContext ac, GraphFieldContainer newContainer, FieldContainer newContent,
		   	GraphFieldSchemaContainerVersion<?, ?, ?, ?, ?> fromVersion,
		   	HibFieldSchemaVersionElement newVersion, Set<String> touchedFields) throws Exception {

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
	protected <T> List<Exception> migrateLoop(Iterable<T> containers, EventCauseInfo cause, MigrationStatusHandler status,
		TriConsumer<EventQueueBatch, T, List<Exception>> migrator) {
		// Iterate over all containers and invoke a migration for each one
		long count = 0;
		List<Exception> errorsDetected = new ArrayList<>();
		EventQueueBatch sqb = batchProvider.get();
		sqb.setCause(cause);
		for (T container : containers) {
			try {
				// Each container migration has its own search queue batch which is then combined with other batch entries.
				// This prevents adding partial entries from failed migrations.
				EventQueueBatch containerBatch = batchProvider.get();
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
	 * Invoke the post migration purge for the containers.
	 *
	 * @param container
	 *            Draft container. May also be published container
	 * @param oldPublished
	 *            Optional published container
	 */
	protected void postMigrationPurge(NodeGraphFieldContainer container, NodeGraphFieldContainer oldPublished) {
		ContentDaoWrapper contentDao = Tx.get().data().contentDao();

		// The purge operation was suppressed before. We need to invoke it now
		// Purge the old publish container if it did not match the draft container. In this case we need to purge the published container dedicatedly.
		if (oldPublished != null && !oldPublished.equals(container) && oldPublished.isAutoPurgeEnabled() && contentDao.isPurgeable(oldPublished)) {
			log.debug("Removing old published container {" + oldPublished.getUuid() + "}");
			contentDao.purge(oldPublished);
		}
		// Now we can purge the draft container (which may also be the published container)
		if (container.isAutoPurgeEnabled() && contentDao.isPurgeable(container)) {
			log.debug("Removing source container {" + container.getUuid() + "}");
			contentDao.purge(container);
		}
	}
}
