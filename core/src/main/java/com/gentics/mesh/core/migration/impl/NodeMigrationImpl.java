package com.gentics.mesh.core.migration.impl;

import static com.gentics.mesh.core.rest.MeshEvent.SCHEMA_MIGRATION_START;
import static com.gentics.mesh.core.rest.common.ContainerType.DRAFT;
import static com.gentics.mesh.core.rest.common.ContainerType.PUBLISHED;
import static com.gentics.mesh.core.rest.error.Errors.error;
import static com.gentics.mesh.core.rest.job.JobStatus.COMPLETED;
import static com.gentics.mesh.core.rest.job.JobStatus.RUNNING;
import static com.gentics.mesh.core.rest.job.JobStatus.STARTING;
import static com.gentics.mesh.metric.SimpleMetric.NODE_MIGRATION_PENDING;
import static io.netty.handler.codec.http.HttpResponseStatus.BAD_REQUEST;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.inject.Inject;
import javax.inject.Provider;
import javax.inject.Singleton;

import com.gentics.mesh.context.NodeMigrationActionContext;
import com.gentics.mesh.context.impl.NodeMigrationActionContextImpl;
import com.gentics.mesh.core.data.HibField;
import com.gentics.mesh.core.data.HibNodeFieldContainer;
import com.gentics.mesh.core.data.branch.HibBranch;
import com.gentics.mesh.core.data.branch.HibBranchSchemaVersion;
import com.gentics.mesh.core.data.dao.ContentDao;
import com.gentics.mesh.core.data.dao.NodeDao;
import com.gentics.mesh.core.data.dao.SchemaDao;
import com.gentics.mesh.core.data.job.HibJob;
import com.gentics.mesh.core.data.node.HibNode;
import com.gentics.mesh.core.data.project.HibProject;
import com.gentics.mesh.core.data.schema.HibSchema;
import com.gentics.mesh.core.data.schema.HibSchemaChange;
import com.gentics.mesh.core.data.schema.HibSchemaVersion;
import com.gentics.mesh.core.db.CommonTx;
import com.gentics.mesh.core.db.Database;
import com.gentics.mesh.core.db.Tx;
import com.gentics.mesh.core.endpoint.migration.MigrationStatusHandler;
import com.gentics.mesh.core.endpoint.node.BinaryUploadHandlerImpl;
import com.gentics.mesh.core.migration.AbstractMigrationHandler;
import com.gentics.mesh.core.migration.NodeMigration;
import com.gentics.mesh.core.rest.common.FieldContainer;
import com.gentics.mesh.core.rest.event.node.SchemaMigrationCause;
import com.gentics.mesh.core.verticle.handler.WriteLock;
import com.gentics.mesh.etc.config.MeshOptions;
import com.gentics.mesh.event.EventQueueBatch;
import com.gentics.mesh.metric.MetricsService;
import com.gentics.mesh.util.VersionNumber;

import io.reactivex.Completable;
import io.reactivex.exceptions.CompositeException;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import org.apache.commons.lang3.tuple.Pair;

/**
 * Handler for node migrations after schema updates.
 */
@Singleton
public class NodeMigrationImpl extends AbstractMigrationHandler implements NodeMigration {

	private static final Logger log = LoggerFactory.getLogger(NodeMigrationImpl.class);

	private final AtomicLong migrationGauge;
	private final WriteLock writeLock;

	@Inject
	public NodeMigrationImpl(Database db, BinaryUploadHandlerImpl nodeFieldAPIHandler, MetricsService metrics, Provider<EventQueueBatch> batchProvider,
							 WriteLock writeLock, MeshOptions options) {
		super(db, nodeFieldAPIHandler, metrics, batchProvider, options);
		migrationGauge = metrics.longGauge(NODE_MIGRATION_PENDING);
		this.writeLock = writeLock;
	}

	@Override
	public NodeMigrationActionContextImpl prepareContext(HibJob job) {
		MigrationStatusHandler status = new MigrationStatusHandlerImpl(job.getUuid());
		try {
			return db.tx(tx -> {
				HibJob hibJob = CommonTx.get().jobDao().mergeIntoPersisted(job);
				NodeMigrationActionContextImpl context = new NodeMigrationActionContextImpl();
				context.setStatus(status);

				tx.createBatch().add(createEvent(hibJob, tx, SCHEMA_MIGRATION_START, STARTING)).dispatch();

				HibBranch branch = hibJob.getBranch();
				if (branch == null) {
					throw error(BAD_REQUEST, "Branch for job {" + hibJob.getUuid() + "} not found");
				}
				context.setBranch(branch);

				HibSchemaVersion fromContainerVersion = hibJob.getFromSchemaVersion();
				if (fromContainerVersion == null) {
					throw error(BAD_REQUEST, "Source schema version for job {" + hibJob.getUuid() + "} could not be found.");
				}
				fromContainerVersion.getChanges().forEach(HibSchemaChange::getRestProperties); // initialize proxies
				context.setFromVersion(fromContainerVersion);

				HibSchemaVersion toContainerVersion = hibJob.getToSchemaVersion();
				if (toContainerVersion == null) {
					throw error(BAD_REQUEST, "Target schema version for job {" + hibJob.getUuid() + "} could not be found.");
				}
				context.setToVersion(toContainerVersion);

				HibSchema schemaContainer = toContainerVersion.getSchemaContainer();
				if (schemaContainer == null) {
					throw error(BAD_REQUEST, "Schema container for job {" + hibJob.getUuid() + "} can't be found.");
				}

				HibProject project = branch.getProject();
				if (project == null) {
					throw error(BAD_REQUEST, "Project for job {" + hibJob.getUuid() + "} not found");
				}
				context.setProject(project);

				HibBranchSchemaVersion branchVersionAssignment = Tx.get().branchDao().findBranchSchemaEdge(branch, toContainerVersion);
				context.getStatus().setVersionEdge(branchVersionAssignment);

				log.info("Handling node migration request for schema {" + schemaContainer.getUuid() + "} from version {"
						+ fromContainerVersion.getUuid() + "} to version {" + toContainerVersion.getUuid() + "} for release {" + branch.getUuid()
						+ "} in project {" + project.getUuid() + "}");

				SchemaMigrationCause cause = new SchemaMigrationCause();
				cause.setFromVersion(fromContainerVersion.transformToReference());
				cause.setToVersion(toContainerVersion.transformToReference());
				cause.setProject(project.transformToReference());
				cause.setBranch(branch.transformToReference());
				cause.setOrigin(tx.data().options().getNodeName());
				cause.setUuid(hibJob.getUuid());
				context.setCause(cause);

				context.getStatus().commit();
				return context;
			});
		} catch (Exception e) {
			db.tx(() -> {
				status.error(e, "Error while preparing node migration.");
			});
			throw e;
		}
	}

	@Override
	public Completable migrateNodes(NodeMigrationActionContext context) {
		context.validate();
		return Completable.defer(() -> {
			HibSchemaVersion fromVersion = context.getFromVersion();
			SchemaMigrationCause cause = context.getCause();
			HibBranch branch = context.getBranch();
			MigrationStatusHandler status = context.getStatus();

			// Prepare the migration - Collect the migration scripts
			Set<String> touchedFields = new HashSet<>();
			try {
				db.tx(() -> {
					prepareMigration(reloadVersion(fromVersion), touchedFields);
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
			Queue<? extends HibNodeFieldContainer> containers = db.tx(tx -> {
				SchemaDao schemaDao = tx.schemaDao();
				return schemaDao.findDraftFieldContainers(fromVersion, branch.getUuid()).stream()
						.collect(Collectors.toCollection(ArrayDeque::new));
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

			List<Exception> errorsDetected = migrateLoop(containers, cause, status, (batch, containerList, errors) -> {
				try (WriteLock lock = writeLock.lock(context)) {
					beforeBatchMigration(containerList, context);
					List<Pair<HibNodeFieldContainer, HibNodeFieldContainer>> toPurge = new ArrayList<>();
					for (HibNodeFieldContainer container : containerList) {
						Pair<HibNodeFieldContainer, HibNodeFieldContainer> toPurgePair = migrateContainer(context, batch, container, errors, touchedFields);
						if (toPurgePair != null) {
							toPurge.add(toPurgePair);
						}
					}

					List<HibNodeFieldContainer> toPurgeList = filterPurgeable(toPurge);
					bulkPurge(toPurgeList);
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

	public List<HibNodeFieldContainer> filterPurgeable(List<Pair<HibNodeFieldContainer, HibNodeFieldContainer>> containerList) {
		ContentDao contentDao = Tx.get().contentDao();

		// perform bulk purge
		return containerList.stream()
				.flatMap(pair -> {
					if (!pair.getLeft().equals(pair.getRight()) && pair.getRight() != null) {
						return Stream.of(pair.getLeft(), pair.getRight());
					}

					return Stream.of(pair.getLeft());
				}).filter(container -> contentDao.isAutoPurgeEnabled(container) && contentDao.isPurgeable(container))
				.collect(Collectors.toList());
	}

	@Override
	public void bulkPurge(List<HibNodeFieldContainer> toPurge) {
		ContentDao contentDao = Tx.get().contentDao();

		toPurge.forEach(contentDao::purge);
	}

	/**
	 * Migrates the given container.
	 * 
	 * @param ac
	 * @param batch
	 * @param container
	 *            Container to be migrated
	 * @param errorsDetected
	 * @param touchedFields
	 * @return a pair with the container and its parent to purge
	 */
	private Pair<HibNodeFieldContainer, HibNodeFieldContainer> migrateContainer(NodeMigrationActionContext ac, EventQueueBatch batch, HibNodeFieldContainer container,
																				List<Exception> errorsDetected, Set<String> touchedFields) {
		ContentDao contentDao = Tx.get().contentDao();

		String containerUuid = container.getUuid();
		HibNode node = contentDao.getNode(container);
		String parentNodeUuid = node.getUuid();
		if (log.isDebugEnabled()) {
			log.debug("Migrating container {" + containerUuid + "} of node {" + parentNodeUuid + "}");
		}

		HibBranch branch = reloadBranch(ac.getBranch());
		HibSchemaVersion toVersion = reloadVersion(ac.getToVersion());
		HibSchemaVersion fromVersion = reloadVersion(ac.getFromVersion());
		try {
			String languageTag = container.getLanguageTag();
			ac.getNodeParameters().setLanguages(languageTag);
			ac.getVersioningParameters().setVersion("draft");

			VersionNumber nextDraftVersion = null;
			HibNodeFieldContainer oldPublished = contentDao.getFieldContainer(node, languageTag, branch.getUuid(), PUBLISHED);

			// 1. Check whether there is any other published container which we need to handle separately
			if (oldPublished != null && !oldPublished.equals(container)) {
				// We only need to migrate the container if the container's schema version is also "old"
				boolean hasSameOldSchemaVersion = container != null
					&& container.getSchemaContainerVersion().getId().equals(container.getSchemaContainerVersion().getId());
				if (hasSameOldSchemaVersion) {
					nextDraftVersion = migratePublishedContainer(ac, batch, branch, node, oldPublished, fromVersion, toVersion, touchedFields);
					nextDraftVersion = nextDraftVersion.nextDraft();
				}

			}
			// 2. Migrate the draft container. This will also update the draft edge.
			migrateDraftContainer(ac, batch, branch, node, container, fromVersion, toVersion, touchedFields, nextDraftVersion);

			return Pair.of(container, oldPublished);
		} catch (Exception e1) {
			log.error("Error while handling container {" + containerUuid + "} of node {" + parentNodeUuid + "} during schema migration.", e1);
			errorsDetected.add(e1);
		}

		return null;
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
	 * @param nextDraftVersion
	 *            Suggested new draft version
	 * @throws Exception
	 */
	private void migrateDraftContainer(NodeMigrationActionContext ac, EventQueueBatch sqb, HibBranch branch, HibNode node,
			HibNodeFieldContainer container, HibSchemaVersion fromVersion, HibSchemaVersion toVersion,
			Set<String> touchedFields, VersionNumber nextDraftVersion) throws Exception {
		NodeDao nodeDao = Tx.get().nodeDao();
		ContentDao contentDao = Tx.get().contentDao();

		String branchUuid = branch.getUuid();
		String languageTag = container.getLanguageTag();

		// Check whether the same container is also used as a published version within the given branch.
		// A migration is always scoped to a specific branch.
		// We need to ensure that the migrated container is also published.
		boolean publish = contentDao.isPublished(container, branchUuid);

		ac.getVersioningParameters().setVersion(container.getVersion().getFullVersion());
		ac.getGenericParameters().setFields("fields");
		FieldContainer fieldContainer = () -> contentDao.getFieldMap(container, ac, container.getSchemaContainerVersion().getSchema(), 0, Collections.singletonList(languageTag));

		// Create field container
		HibNodeFieldContainer migrated = contentDao.createEmptyFieldContainer(toVersion, node, container.getEditor(), languageTag, branch);
		cloneUntouchedFields(container, migrated, touchedFields);

		// Ensure that the migrated version is also published since the old version was
		if (publish) {
			contentDao.setVersion(migrated, container.getVersion().nextPublished());
			nodeDao.setPublished(node, ac, migrated, branchUuid);
		} else {
			if (nextDraftVersion == null) {
				nextDraftVersion = container.getVersion().nextDraft();
			}
			contentDao.setVersion(migrated, nextDraftVersion);
		}

		// apply changes
		migrate(ac, migrated, fieldContainer, fromVersion);

		// Ensure the search index is updated accordingly
		sqb.add(contentDao.onUpdated(migrated, branchUuid, DRAFT));
		if (publish) {
			sqb.add(contentDao.onUpdated(migrated, branchUuid, PUBLISHED));
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
	 * @return Version of the new published container
	 * @throws Exception
	 */
	private VersionNumber migratePublishedContainer(NodeMigrationActionContext ac, EventQueueBatch sqb, HibBranch branch, HibNode node,
		HibNodeFieldContainer content, HibSchemaVersion fromVersion, HibSchemaVersion toVersion,
		Set<String> touchedFields) throws Exception {
		NodeDao nodeDao = Tx.get().nodeDao();
		ContentDao contentDao = Tx.get().contentDao();

		String languageTag = content.getLanguageTag();
		String branchUuid = branch.getUuid();

		ac.getVersioningParameters().setVersion("published");
		ac.getGenericParameters().setFields("fields");

		FieldContainer fieldContainer = () -> contentDao.getFieldMap(content, ac, content.getSchemaContainerVersion().getSchema(), 0, Collections.singletonList(languageTag));

		HibNodeFieldContainer migrated = contentDao.createEmptyFieldContainer(toVersion, node, content.getEditor(), languageTag, branch);
		cloneUntouchedFields(content, migrated, touchedFields);

		contentDao.setVersion(migrated, content.getVersion().nextPublished());
		nodeDao.setPublished(node, ac, migrated, branchUuid);

		migrate(ac, migrated, fieldContainer, fromVersion);
		sqb.add(contentDao.onUpdated(migrated, branchUuid, PUBLISHED));
		return migrated.getVersion();
	}

	private void cloneUntouchedFields(HibNodeFieldContainer oldContainer, HibNodeFieldContainer newContainer, Set<String> touchedFields) {
		List<HibField> otherFields = oldContainer.getFields();
		for (HibField graphField : otherFields) {
			if (!touchedFields.contains(graphField.getFieldKey())) {
				graphField.cloneTo(newContainer);
			}
		}
	}
}
