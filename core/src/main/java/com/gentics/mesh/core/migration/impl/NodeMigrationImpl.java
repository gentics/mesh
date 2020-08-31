package com.gentics.mesh.core.migration.impl;

import static com.gentics.mesh.core.rest.common.ContainerType.DRAFT;
import static com.gentics.mesh.core.rest.common.ContainerType.PUBLISHED;
import static com.gentics.mesh.core.rest.job.JobStatus.COMPLETED;
import static com.gentics.mesh.core.rest.job.JobStatus.RUNNING;
import static com.gentics.mesh.metric.SimpleMetric.NODE_MIGRATION_PENDING;

import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicLong;

import javax.inject.Inject;
import javax.inject.Provider;
import javax.inject.Singleton;

import com.gentics.mesh.context.NodeMigrationActionContext;
import com.gentics.mesh.core.data.NodeGraphFieldContainer;
import com.gentics.mesh.core.data.branch.HibBranch;
import com.gentics.mesh.core.data.dao.ContentDaoWrapper;
import com.gentics.mesh.core.data.dao.NodeDaoWrapper;
import com.gentics.mesh.core.data.dao.SchemaDaoWrapper;
import com.gentics.mesh.core.data.node.HibNode;
import com.gentics.mesh.core.data.schema.HibSchemaVersion;
import com.gentics.mesh.core.db.Tx;
import com.gentics.mesh.core.endpoint.migration.MigrationStatusHandler;
import com.gentics.mesh.core.endpoint.node.BinaryUploadHandler;
import com.gentics.mesh.core.migration.AbstractMigrationHandler;
import com.gentics.mesh.core.migration.NodeMigration;
import com.gentics.mesh.core.rest.event.node.SchemaMigrationCause;
import com.gentics.mesh.core.rest.node.NodeResponse;
import com.gentics.mesh.core.rest.schema.SchemaVersionModel;
import com.gentics.mesh.core.verticle.handler.WriteLock;
import com.gentics.mesh.event.EventQueueBatch;
import com.gentics.mesh.graphdb.spi.Database;
import com.gentics.mesh.metric.MetricsService;
import com.gentics.mesh.util.VersionNumber;
import com.google.common.collect.Lists;

import io.reactivex.Completable;
import io.reactivex.exceptions.CompositeException;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;

/**
 * Handler for node migrations after schema updates.
 */
@Singleton
public class NodeMigrationImpl extends AbstractMigrationHandler implements NodeMigration {

	private static final Logger log = LoggerFactory.getLogger(NodeMigrationImpl.class);

	private final AtomicLong migrationGauge;
	private final WriteLock writeLock;

	@Inject
	public NodeMigrationImpl(Database db, BinaryUploadHandler nodeFieldAPIHandler, MetricsService metrics, Provider<EventQueueBatch> batchProvider,
		WriteLock writeLock) {
		super(db, nodeFieldAPIHandler, metrics, batchProvider);
		migrationGauge = metrics.longGauge(NODE_MIGRATION_PENDING);
		this.writeLock = writeLock;
	}

	@Override
	public Completable migrateNodes(NodeMigrationActionContext context) {
		context.validate();
		return Completable.defer(() -> {
			HibSchemaVersion fromVersion = context.getFromVersion();
			HibSchemaVersion toVersion = context.getToVersion();
			SchemaMigrationCause cause = context.getCause();
			HibBranch branch = context.getBranch();
			MigrationStatusHandler status = context.getStatus();

			// Prepare the migration - Collect the migration scripts
			Set<String> touchedFields = new HashSet<>();
			SchemaVersionModel newSchema = db.tx(() -> toVersion.getSchema());

			try {
				db.tx(() -> {
					prepareMigration(fromVersion, touchedFields);
					if (status != null) {
						status.setStatus(RUNNING);
						status.commit();
					}
				});
			} catch (Exception e) {
				log.error("Error while preparing migration");
				return Completable.error(e);
			}

			// Get the draft containers that need to be transformed. Containers which need to be transformed are those which are still linked to older schema
			// versions. We'll work on drafts. The migration code will later on also handle publish versions.
			List<? extends NodeGraphFieldContainer> containers = db.tx(tx -> {
				SchemaDaoWrapper schemaDao = tx.data().schemaDao();
				Iterator<? extends NodeGraphFieldContainer> it = schemaDao.findDraftFieldContainers(fromVersion, branch.getUuid());
				return Lists.newArrayList(it);
			});

			if (metrics.isEnabled()) {
				migrationGauge.set(containers.size());
			}

			// No field containers, migration is done
			if (containers.isEmpty()) {
				if (status != null) {
					db.tx(() -> {
						status.setStatus(COMPLETED);
						status.commit();
					});
				}
				return Completable.complete();
			}

			List<Exception> errorsDetected = migrateLoop(containers, cause, status, (batch, container, errors) -> {
				try (WriteLock lock = writeLock.lock(context)) {
					migrateContainer(context, batch, container, fromVersion, newSchema, errors, touchedFields);
				}
				if (metrics.isEnabled()) {
					migrationGauge.decrementAndGet();
				}
			});

			// TODO prepare errors. They should be easy to understand and to grasp
			Completable result = Completable.complete();
			if (!errorsDetected.isEmpty()) {
				if (log.isDebugEnabled()) {
					for (Exception error : errorsDetected) {
						log.error("Encountered migration error.", error);
					}
				}
				result = Completable.error(new CompositeException(errorsDetected));
			}
			return result;
		});

	}

	/**
	 * Migrates the given container.
	 * 
	 * @param ac
	 * @param batch
	 * @param container
	 *            Container to be migrated
	 * @param fromVersion
	 * @param newSchema
	 * @param errorsDetected
	 * @param touchedFields
	 * @return
	 */
	private void migrateContainer(NodeMigrationActionContext ac, EventQueueBatch batch, NodeGraphFieldContainer container,
		HibSchemaVersion fromVersion, SchemaVersionModel newSchema, List<Exception> errorsDetected,
		Set<String> touchedFields) {
		ContentDaoWrapper contentDao = Tx.get().data().contentDao();

		String containerUuid = container.getUuid();
		String parentNodeUuid = contentDao.getNode(container).getUuid();
		if (log.isDebugEnabled()) {
			log.debug("Migrating container {" + containerUuid + "} of node {" + parentNodeUuid + "}");
		}

		HibBranch branch = ac.getBranch();
		HibSchemaVersion toVersion = ac.getToVersion();
		try {
			HibNode node = contentDao.getNode(container);
			String languageTag = container.getLanguageTag();
			ac.getNodeParameters().setLanguages(languageTag);
			ac.getVersioningParameters().setVersion("draft");

			VersionNumber nextDraftVersion = null;
			NodeGraphFieldContainer oldPublished = contentDao.getGraphFieldContainer(node, languageTag, branch.getUuid(), PUBLISHED);

			// 1. Check whether there is any other published container which we need to handle separately
			if (oldPublished != null && !oldPublished.equals(container)) {
				// We only need to migrate the container if the container's schema version is also "old"
				boolean hasSameOldSchemaVersion = container != null
					&& container.getSchemaContainerVersion().id().equals(container.getSchemaContainerVersion().id());
				if (hasSameOldSchemaVersion) {
					nextDraftVersion = migratePublishedContainer(ac, batch, branch, node, oldPublished, fromVersion, toVersion, touchedFields,
						newSchema);
					nextDraftVersion = nextDraftVersion.nextDraft();
				}

			}
			// 2. Migrate the draft container. This will also update the draft edge.
			migrateDraftContainer(ac, batch, branch, node, container, fromVersion, toVersion, touchedFields, newSchema, nextDraftVersion);

			postMigrationPurge(container, oldPublished);
		} catch (Exception e1) {
			log.error("Error while handling container {" + containerUuid + "} of node {" + parentNodeUuid + "} during schema migration.", e1);
			errorsDetected.add(e1);
		}

	}

	/**
	 * Migrate the given container.
	 * 
	 * @param ac
	 * @param sqb
	 *            Batch to be updated to handle index changes
	 * @param branch
	 *            Branch in which the migration is running
	 * @param node
	 *            Node of the container
	 * @param container
	 *            Container to be migrated
	 * @param fromVersion
	 * @param toVersion
	 * @param touchedFields
	 * @param newSchema
	 *            new schema used to serialize the REST model
	 * @param nextDraftVersion
	 *            Suggested new draft version
	 * @throws Exception
	 */
	private void migrateDraftContainer(NodeMigrationActionContext ac, EventQueueBatch sqb, HibBranch branch, HibNode node,
		NodeGraphFieldContainer container, HibSchemaVersion fromVersion, HibSchemaVersion toVersion,
		Set<String> touchedFields,
		SchemaVersionModel newSchema, VersionNumber nextDraftVersion)
		throws Exception {
		NodeDaoWrapper nodeDao = Tx.get().data().nodeDao();
		ContentDaoWrapper contentDao = Tx.get().data().contentDao();

		String branchUuid = branch.getUuid();
		String languageTag = container.getLanguageTag();

		// Check whether the same container is also used as a published version within the given branch.
		// A migration is always scoped to a specific branch.
		// We need to ensure that the migrated container is also published.
		boolean publish = container.isPublished(branchUuid);

		ac.getVersioningParameters().setVersion(container.getVersion().getFullVersion());
		ac.getGenericParameters().setFields("fields");
		NodeResponse restModel = nodeDao.transformToRestSync(node, ac, 0, languageTag);

		// Actual migration - Create the new version
		NodeGraphFieldContainer migrated = contentDao.createGraphFieldContainer(node, container.getLanguageTag(), branch, container.getEditor(),
			container, true);

		// Ensure that the migrated version is also published since the old version was
		if (publish) {
			migrated.setVersion(container.getVersion().nextPublished());
			nodeDao.setPublished(node, ac, migrated, branchUuid);
		} else {
			if (nextDraftVersion == null) {
				nextDraftVersion = container.getVersion().nextDraft();
			}
			migrated.setVersion(nextDraftVersion);
		}

		// Pass the new version through the migration scripts and update the version
		migrate(ac, migrated, restModel, fromVersion, toVersion, touchedFields);

		// Ensure the search index is updated accordingly
		sqb.add(migrated.onUpdated(branchUuid, DRAFT));
		if (publish) {
			sqb.add(migrated.onUpdated(branchUuid, PUBLISHED));
		}
	}

	/**
	 * Migrate the published container.
	 * 
	 * @param ac
	 * @param sqb
	 *            Batch to be used to update the search index
	 * @param branch
	 * @param node
	 *            Node of the container
	 * @param content
	 *            Container to be migrated
	 * @param fromVersion
	 * @param toVersion
	 * @param touchedFields
	 * @param newSchema
	 * @return Version of the new published container
	 * @throws Exception
	 */
	private VersionNumber migratePublishedContainer(NodeMigrationActionContext ac, EventQueueBatch sqb, HibBranch branch, HibNode node,
		NodeGraphFieldContainer content, HibSchemaVersion fromVersion, HibSchemaVersion toVersion,
		Set<String> touchedFields, SchemaVersionModel newSchema) throws Exception {
		NodeDaoWrapper nodeDao = Tx.get().data().nodeDao();
		ContentDaoWrapper contentDao = Tx.get().data().contentDao();

		String languageTag = content.getLanguageTag();
		String branchUuid = branch.getUuid();

		ac.getVersioningParameters().setVersion("published");
		ac.getGenericParameters().setFields("fields");

		NodeResponse restModel = nodeDao.transformToRestSync(node, ac, 0, languageTag);

		NodeGraphFieldContainer migrated = contentDao.createGraphFieldContainer(node, content.getLanguageTag(), branch, content.getEditor(),
			content, true);

		migrated.setVersion(content.getVersion().nextPublished());
		nodeDao.setPublished(node, ac, migrated, branchUuid);

		migrate(ac, migrated, restModel, fromVersion, toVersion, touchedFields);
		sqb.add(migrated.onUpdated(branchUuid, PUBLISHED));
		return migrated.getVersion();
	}

}
