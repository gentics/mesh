package com.gentics.mesh.core.migration.impl;

import static com.gentics.mesh.core.rest.common.ContainerType.DRAFT;
import static com.gentics.mesh.core.rest.common.ContainerType.PUBLISHED;
import static com.gentics.mesh.core.rest.job.JobStatus.COMPLETED;
import static com.gentics.mesh.core.rest.job.JobStatus.RUNNING;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.inject.Inject;
import javax.inject.Provider;
import javax.inject.Singleton;

import com.gentics.mesh.context.MicronodeMigrationContext;
import com.gentics.mesh.context.impl.NodeMigrationActionContextImpl;
import com.gentics.mesh.core.data.HibNodeFieldContainer;
import com.gentics.mesh.core.data.NodeGraphFieldContainer;
import com.gentics.mesh.core.data.branch.HibBranch;
import com.gentics.mesh.core.data.dao.ContentDao;
import com.gentics.mesh.core.data.dao.MicroschemaDaoWrapper;
import com.gentics.mesh.core.data.dao.NodeDaoWrapper;
import com.gentics.mesh.core.data.node.HibMicronode;
import com.gentics.mesh.core.data.node.HibNode;
import com.gentics.mesh.core.data.node.field.list.HibMicronodeFieldList;
import com.gentics.mesh.core.data.node.field.nesting.HibMicronodeField;
import com.gentics.mesh.core.data.schema.HibMicroschemaVersion;
import com.gentics.mesh.core.db.Database;
import com.gentics.mesh.core.db.Tx;
import com.gentics.mesh.core.endpoint.migration.MigrationStatusHandler;
import com.gentics.mesh.core.endpoint.node.BinaryUploadHandlerImpl;
import com.gentics.mesh.core.migration.AbstractMigrationHandler;
import com.gentics.mesh.core.migration.MicronodeMigration;
import com.gentics.mesh.core.rest.event.node.MicroschemaMigrationCause;
import com.gentics.mesh.core.rest.micronode.MicronodeResponse;
import com.gentics.mesh.core.verticle.handler.WriteLock;
import com.gentics.mesh.event.EventQueueBatch;
import com.gentics.mesh.metric.MetricsService;
import com.gentics.mesh.util.VersionNumber;

import io.reactivex.Completable;
import io.reactivex.exceptions.CompositeException;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;

/**
 * @see MicronodeMigration
 */
@Singleton
public class MicronodeMigrationImpl extends AbstractMigrationHandler implements MicronodeMigration {

	private static final Logger log = LoggerFactory.getLogger(MicronodeMigrationImpl.class);

	private final WriteLock writeLock;

	@Inject
	public MicronodeMigrationImpl(Database db, BinaryUploadHandlerImpl binaryFieldHandler, MetricsService metrics, Provider<EventQueueBatch> batchProvider, WriteLock writeLock) {
		super(db, binaryFieldHandler, metrics, batchProvider);
		this.writeLock = writeLock;
	}

	@Override
	public Completable migrateMicronodes(MicronodeMigrationContext context) {
		context.validate();
		return Completable.defer(() -> {
			HibBranch branch = context.getBranch();
			HibMicroschemaVersion fromVersion = context.getFromVersion();
			HibMicroschemaVersion toVersion = context.getToVersion();
			MigrationStatusHandler status = context.getStatus();
			MicroschemaMigrationCause cause = context.getCause();

			// Collect the migration scripts
			NodeMigrationActionContextImpl ac = new NodeMigrationActionContextImpl();
			Set<String> touchedFields = new HashSet<>();
			try {
				db.tx(() -> {
					prepareMigration(fromVersion, touchedFields);

					ac.setProject(branch.getProject());
					ac.setBranch(branch);

					if (status != null) {
						status.setStatus(RUNNING);
						status.commit();
					}
				});
			} catch (Exception e) {
				return Completable.error(e);
			}

			// Get the containers, that need to be transformed
			List<? extends NodeGraphFieldContainer> fieldContainersResult = db.tx(tx -> {
				MicroschemaDaoWrapper microschemaDao = (MicroschemaDaoWrapper) tx.microschemaDao();
				return microschemaDao.findDraftFieldContainers(fromVersion, branch.getUuid()).list();
			});

			// No field containers, migration is done
			if (fieldContainersResult.isEmpty()) {
				if (status != null) {
					db.tx(() -> {
						status.setStatus(COMPLETED);
						status.commit();
					});
				}
				return Completable.complete();
			}

			List<Exception> errorsDetected = migrateLoop(fieldContainersResult, cause, status,
				(batch, container, errors) -> {
					try (WriteLock lock = writeLock.lock(ac)) {
						migrateMicronodeContainer(ac, batch, branch, fromVersion, toVersion, container, touchedFields, errors);
					}
				});

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
	 * Migrate the draft container. Internally we will also check whether the container is also the published container and handle this case correctly.
	 *
	 * @param ac
	 * @param sqb
	 * @param branch
	 * @param node
	 * @param container
	 * @param fromVersion
	 * @param toVersion
	 * @param touchedFields
	 * @param nextDraftVersion
	 * @throws Exception
	 */
	private void migrateDraftContainer(NodeMigrationActionContextImpl ac, EventQueueBatch sqb, HibBranch branch, HibNode node,
		NodeGraphFieldContainer container, HibMicroschemaVersion fromVersion, HibMicroschemaVersion toVersion,
		Set<String> touchedFields, VersionNumber nextDraftVersion)
		throws Exception {
		NodeDaoWrapper nodeDao = (NodeDaoWrapper) Tx.get().nodeDao();
		ContentDao contentDao = Tx.get().contentDao();

		String branchUuid = branch.getUuid();
		ac.getVersioningParameters().setVersion(container.getVersion().getFullVersion());

		boolean publish = container.isPublished(branchUuid);

		// Clone the field container. This will also update the draft edge
		HibNodeFieldContainer migrated = contentDao.createFieldContainer(node, container.getLanguageTag(), branch, container.getEditor(), container, true);
		if (publish) {
			migrated.setVersion(container.getVersion().nextPublished());
			// Ensure that the publish edge is also updated correctly
			nodeDao.setPublished(node, ac, migrated, branchUuid);
		} else {
			if (nextDraftVersion == null) {
				nextDraftVersion = container.getVersion().nextDraft();
			}
			migrated.setVersion(nextDraftVersion);
		}

		migrateMicronodeFields(ac, migrated, fromVersion, toVersion, touchedFields);

		// Ensure the search index is updated accordingly
		sqb.add(migrated.onUpdated(branchUuid, DRAFT));
		if (publish) {
			sqb.add(migrated.onUpdated(branchUuid, PUBLISHED));
		}
	}

	/**
	 * Migrate the given micronode container.
	 *
	 * @param ac
	 * @param batch
	 * @param branch
	 * @param fromVersion
	 * @param toVersion
	 * @param container
	 * @param touchedFields
	 * @param errorsDetected
	 */
	private void migrateMicronodeContainer(NodeMigrationActionContextImpl ac, EventQueueBatch batch, HibBranch branch,
										   HibMicroschemaVersion fromVersion,
										   HibMicroschemaVersion toVersion, NodeGraphFieldContainer container, Set<String> touchedFields,
										   List<Exception> errorsDetected) {
		String containerUuid = container.getUuid();

		if (log.isDebugEnabled()) {
			log.debug("Migrating container {" + containerUuid + "}");
		}
		String branchUuid = branch.getUuid();

		// Run the actual migration in a dedicated transaction
		try {
			db.tx(tx -> {
				ContentDao contentDao = tx.contentDao();

				HibNode node = contentDao.getNode(container);
				String languageTag = container.getLanguageTag();
				ac.getNodeParameters().setLanguages(languageTag);
				ac.getVersioningParameters().setVersion("draft");
				HibNodeFieldContainer oldPublished = contentDao.getFieldContainer(node, languageTag, branchUuid, PUBLISHED);

				VersionNumber nextDraftVersion = null;
				// 1. Check whether there is any other published container which we need to handle separately
				if (oldPublished != null && !oldPublished.equals(container)) {
					nextDraftVersion = migratePublishedContainer(ac, batch, branch, node, container, fromVersion, toVersion, touchedFields);
					nextDraftVersion = nextDraftVersion.nextDraft();
				}

				// 2. Migrate the draft container. This will also update the draft edge.
				migrateDraftContainer(ac, batch, branch, node, container, fromVersion, toVersion, touchedFields, nextDraftVersion);

				postMigrationPurge(container, oldPublished);
			});
		} catch (Exception e1) {
			log.error("Error while handling container {" + containerUuid + "} during schema migration.", e1);
			errorsDetected.add(e1);
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
	 * @param container
	 *            Container to be migrated
	 * @param fromVersion
	 * @param toVersion
	 * @param touchedFields
	 * @return Version of the new published container
	 * @throws Exception
	 */
	private VersionNumber migratePublishedContainer(NodeMigrationActionContextImpl ac, EventQueueBatch sqb, HibBranch branch, HibNode node,
		NodeGraphFieldContainer container, HibMicroschemaVersion fromVersion, HibMicroschemaVersion toVersion,
		Set<String> touchedFields) throws Exception {
		NodeDaoWrapper nodeDao = (NodeDaoWrapper) Tx.get().nodeDao();
		ContentDao contentDao = Tx.get().contentDao();

		String branchUuid = branch.getUuid();
		ac.getVersioningParameters().setVersion("published");

		HibNodeFieldContainer migrated = contentDao.createFieldContainer(node, container.getLanguageTag(), branch, container.getEditor(), container, true);
		migrated.setVersion(container.getVersion().nextPublished());
		nodeDao.setPublished(node, ac, migrated, branchUuid);

		migrateMicronodeFields(ac, migrated, fromVersion, toVersion, touchedFields);
		sqb.add(migrated.onUpdated(branchUuid, PUBLISHED));
		return migrated.getVersion();

	}

	/**
	 * Migrate all micronode fields from old schema version to new schema version
	 * 
	 * @param ac
	 *            action context
	 * @param container
	 *            field container
	 * @param fromVersion
	 *            old schema version
	 * @param toVersion
	 *            new schema version
	 * @param touchedFields
	 *            touched fields
	 * @throws Exception
	 */
	protected void migrateMicronodeFields(NodeMigrationActionContextImpl ac, HibNodeFieldContainer container,
		HibMicroschemaVersion fromVersion, HibMicroschemaVersion toVersion, Set<String> touchedFields) throws Exception {
		// iterate over all fields with micronodes to migrate
		for (HibMicronodeField field : container.getMicronodeFields(fromVersion)) {
			// clone the field (this will clone the micronode)
			field = container.createMicronode(field.getFieldKey(), fromVersion);
			HibMicronode micronode = field.getMicronode();
			// transform to rest and migrate
			MicronodeResponse restModel = micronode.transformToRestSync(ac, 0);
			migrate(ac, micronode, restModel, fromVersion, toVersion, touchedFields);
		}

		// iterate over all micronode list fields to migrate
		for (HibMicronodeFieldList field : container.getMicronodeListFields(fromVersion)) {
			HibMicronodeFieldList oldListField = field;

			// clone the field (this will not clone the micronodes)
			field = container.createMicronodeFieldList(field.getFieldKey());

			// clone every micronode
			for (HibMicronodeField oldField : oldListField.getList()) {
				HibMicronode oldMicronode = oldField.getMicronode();
				HibMicronode newMicronode = field.createMicronode();
				newMicronode.setSchemaContainerVersion(oldMicronode.getSchemaContainerVersion());
				newMicronode.clone(oldMicronode);

				// migrate the micronode, if it uses the fromVersion
				if (newMicronode.getSchemaContainerVersion().equals(fromVersion)) {
					// transform to rest and migrate
					MicronodeResponse restModel = newMicronode.transformToRestSync(ac, 0);
					migrate(ac, newMicronode, restModel, fromVersion, toVersion, touchedFields);
				}
			}
		}
	}

}
