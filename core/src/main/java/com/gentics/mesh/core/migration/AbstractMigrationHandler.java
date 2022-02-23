package com.gentics.mesh.core.migration;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Set;

import javax.annotation.ParametersAreNonnullByDefault;
import javax.inject.Provider;

import com.gentics.mesh.context.NodeMigrationActionContext;
import com.gentics.mesh.core.data.HibFieldContainer;
import com.gentics.mesh.core.data.HibNodeFieldContainer;
import com.gentics.mesh.core.data.branch.HibBranch;
import com.gentics.mesh.core.data.dao.ContentDao;
import com.gentics.mesh.core.data.dao.PersistingBranchDao;
import com.gentics.mesh.core.data.schema.HibFieldSchemaVersionElement;
import com.gentics.mesh.core.data.schema.HibFieldTypeChange;
import com.gentics.mesh.core.data.schema.HibMicroschemaVersion;
import com.gentics.mesh.core.data.schema.HibRemoveFieldChange;
import com.gentics.mesh.core.data.schema.HibSchemaChange;
import com.gentics.mesh.core.data.schema.HibSchemaVersion;
import com.gentics.mesh.core.db.CommonTx;
import com.gentics.mesh.core.db.Database;
import com.gentics.mesh.core.db.Tx;
import com.gentics.mesh.core.endpoint.handler.AbstractHandler;
import com.gentics.mesh.core.endpoint.migration.MigrationHandler;
import com.gentics.mesh.core.endpoint.migration.MigrationStatusHandler;
import com.gentics.mesh.core.endpoint.migration.TriConsumer;
import com.gentics.mesh.core.endpoint.node.BinaryUploadHandlerImpl;
import com.gentics.mesh.core.rest.common.FieldContainer;
import com.gentics.mesh.core.rest.event.EventCauseInfo;
import com.gentics.mesh.core.rest.node.FieldMap;
import com.gentics.mesh.core.rest.node.FieldMapImpl;
import com.gentics.mesh.core.rest.node.field.Field;
import com.gentics.mesh.event.EventQueueBatch;
import com.gentics.mesh.metric.MetricsService;
import com.gentics.mesh.util.StreamUtil;

import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;

/**
 * Abstract implementation for migration handlers that deal with content migrations.
 */
public abstract class AbstractMigrationHandler extends AbstractHandler implements MigrationHandler {

	private static final Logger log = LoggerFactory.getLogger(AbstractMigrationHandler.class);

	protected Database db;

	protected BinaryUploadHandlerImpl binaryFieldHandler;

	protected MetricsService metrics;

	protected final Provider<EventQueueBatch> batchProvider;

	public AbstractMigrationHandler(Database db, BinaryUploadHandlerImpl binaryFieldHandler, MetricsService metrics,
		Provider<EventQueueBatch> batchProvider) {
		this.db = db;
		this.binaryFieldHandler = binaryFieldHandler;
		this.metrics = metrics;
		this.batchProvider = batchProvider;
	}

	/**
	 * Collect the set of touched fields when migrating the given container into the next version
	 *
	 * @param fromVersion
	 *            Container which contains the expected migration changes
	 * @param touchedFields
	 *            Set of touched fields (will be modified)
	 */
	protected void prepareMigration(HibFieldSchemaVersionElement<?, ?, ?, ?, ?> fromVersion, Set<String> touchedFields) {
		HibSchemaChange<?> change = fromVersion.getNextChange();
		while (change != null) {
			// if either the type changes or the field is removed, the field is "touched"
			if (change instanceof HibFieldTypeChange) {
				touchedFields.add(((HibFieldTypeChange) change).getFieldName());
			} else if (change instanceof HibRemoveFieldChange) {
				touchedFields.add(((HibRemoveFieldChange) change).getFieldName());
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
	 * @throws Exception
	 */
	protected void migrate(NodeMigrationActionContext ac, HibFieldContainer newContainer, FieldContainer newContent,
						   HibFieldSchemaVersionElement<?, ?, ?, ?, ?> fromVersion) throws Exception {
		FieldMap fields = new FieldMapImpl();
		Map<String, Field> newFields = fromVersion.getChanges()
			.filter(change -> !(change instanceof HibRemoveFieldChange)) // nothing to do for removed fields, they were never added
			.map(change -> change.createFields(fromVersion.getSchema(), newContent))
			.collect(StreamUtil.mergeMaps());

		fields.putAll(newFields);

		newContainer.updateFieldsFromRest(ac, fields);
	}

	/**
	 * Migrate all elements in the given queue, by passing it to the migrator. The elements will be removed from the queue, before they are migrated,
	 * in order to save memory (elements will be "enriched" with additional data during migration).
	 * @param <T> type of migrated elements
	 * @param containers queue of elements to be migrated
	 * @param cause information about the cause
	 * @param status migration status (will be updated during the migration)
	 * @param migrator migrator
	 * @return list of exceptions caught during the migration
	 */
	@ParametersAreNonnullByDefault
	protected <T> List<Exception> migrateLoop(Queue<T> containers, EventCauseInfo cause, MigrationStatusHandler status,
		TriConsumer<EventQueueBatch, T, List<Exception>> migrator) {
		// Iterate over all containers and invoke a migration for each one
		long count = 0;
		List<Exception> errorsDetected = new ArrayList<>();
		EventQueueBatch sqb = batchProvider.get();
		sqb.setCause(cause);
		while (!containers.isEmpty()) {
			T container = containers.poll();
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
	protected void postMigrationPurge(HibNodeFieldContainer container, HibNodeFieldContainer oldPublished) {
		ContentDao contentDao = Tx.get().contentDao();

		// The purge operation was suppressed before. We need to invoke it now
		// Purge the old publish container if it did not match the draft container. In this case we need to purge the published container dedicatedly.
		if (oldPublished != null && !oldPublished.equals(container) && contentDao.isAutoPurgeEnabled(oldPublished) && contentDao.isPurgeable(oldPublished)) {
			log.debug("Removing old published container {" + oldPublished.getUuid() + "}");
			contentDao.purge(oldPublished);
		}
		// Now we can purge the draft container (which may also be the published container)
		if (contentDao.isAutoPurgeEnabled(container) && contentDao.isPurgeable(container)) {
			log.debug("Removing source container {" + container.getUuid() + "}");
			contentDao.purge(container);
		}
	}

	protected HibSchemaVersion reloadVersion(HibSchemaVersion version) {
		return CommonTx.get().load(version.getId(), CommonTx.get().schemaDao().getVersionPersistenceClass());
	}

	protected HibMicroschemaVersion reloadVersion(HibMicroschemaVersion version) {
		return CommonTx.get().load(version.getId(), CommonTx.get().microschemaDao().getVersionPersistenceClass());
	}

	protected HibBranch reloadBranch(HibBranch branch) {
		PersistingBranchDao branchDao = CommonTx.get().branchDao();
		return branchDao.findByUuid(branch.getProject(), branch.getUuid());
	}
}
