package com.gentics.mesh.core.migration.impl;

import static com.gentics.mesh.core.rest.common.ContainerType.DRAFT;
import static com.gentics.mesh.core.rest.common.ContainerType.PUBLISHED;
import static com.gentics.mesh.core.rest.job.JobStatus.COMPLETED;
import static com.gentics.mesh.core.rest.job.JobStatus.RUNNING;

import java.util.ArrayDeque;
import java.util.HashSet;
import java.util.List;
import java.util.Queue;
import java.util.Set;
import java.util.stream.Collectors;

import javax.inject.Inject;
import javax.inject.Provider;
import javax.inject.Singleton;

import com.gentics.mesh.context.MicronodeMigrationContext;
import com.gentics.mesh.context.impl.NodeMigrationActionContextImpl;
import com.gentics.mesh.core.data.HibField;
import com.gentics.mesh.core.data.HibNodeFieldContainer;
import com.gentics.mesh.core.data.branch.HibBranch;
import com.gentics.mesh.core.data.dao.ContentDao;
import com.gentics.mesh.core.data.dao.MicroschemaDao;
import com.gentics.mesh.core.data.dao.NodeDao;
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
import com.gentics.mesh.core.rest.common.FieldTypes;
import com.gentics.mesh.core.rest.event.node.MicroschemaMigrationCause;
import com.gentics.mesh.core.rest.micronode.MicronodeResponse;
import com.gentics.mesh.core.rest.schema.FieldSchema;
import com.gentics.mesh.core.rest.schema.FieldSchemaContainer;
import com.gentics.mesh.core.rest.schema.ListFieldSchema;
import com.gentics.mesh.core.verticle.handler.WriteLock;
import com.gentics.mesh.distributed.RequestDelegator;
import com.gentics.mesh.etc.config.MeshOptions;
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
	public MicronodeMigrationImpl(Database db, BinaryUploadHandlerImpl binaryFieldHandler, MetricsService metrics,
			Provider<EventQueueBatch> batchProvider, WriteLock writeLock, MeshOptions options,
			RequestDelegator delegator) {
		super(db, binaryFieldHandler, metrics, batchProvider, options, delegator);
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
				db.tx(tx -> {
					ac.setHttpServerConfig(tx.data().options().getHttpServerOptions());
					prepareMigration(reloadVersion(fromVersion), touchedFields);
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
			Queue<? extends HibNodeFieldContainer> fieldContainersResult = db.tx(tx -> {
				MicroschemaDao microschemaDao = tx.microschemaDao();
				return microschemaDao.findDraftFieldContainers(fromVersion, branch.getUuid()).stream()
						.collect(Collectors.toCollection(ArrayDeque::new));
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
					(batch, containers, errors) -> {
						try (WriteLock lock = writeLock.lock(ac)) {
							beforeBatchMigration(containers, ac);
							for (HibNodeFieldContainer container : containers) {
								migrateMicronodeContainer(ac, context, batch, container, touchedFields, errors);
							}
						}
					});

			Completable result = Completable.complete();
			if (!errorsDetected.isEmpty()) {
				if (log.isDebugEnabled()) {
					for (Exception error : errorsDetected) {
						log.error("Encountered migration error.", error);
					}
				}
				if (errorsDetected.size() == 1) {
					result = Completable.error(errorsDetected.get(0));
				} else {
					result = Completable.error(new CompositeException(errorsDetected));
				}
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
									   HibNodeFieldContainer container, HibMicroschemaVersion fromVersion, HibMicroschemaVersion toVersion,
									   Set<String> touchedFields, VersionNumber nextDraftVersion)
			throws Exception {
		NodeDao nodeDao = Tx.get().nodeDao();
		ContentDao contentDao = Tx.get().contentDao();

		String branchUuid = branch.getUuid();
		ac.getVersioningParameters().setVersion(container.getVersion().getFullVersion());

		boolean publish = contentDao.isPublished(container, branchUuid);

		// Clone the field container. This will also update the draft edge
		HibNodeFieldContainer migrated = contentDao.createEmptyFieldContainer(reloadVersion(container.getSchemaContainerVersion()), node, container.getEditor(), container.getLanguageTag(), branch);
		if (publish) {
			contentDao.setVersion(migrated, container.getVersion().nextPublished());
			// Ensure that the publish edge is also updated correctly
			nodeDao.setPublished(node, ac, migrated, branchUuid);
		} else {
			if (nextDraftVersion == null) {
				nextDraftVersion = container.getVersion().nextDraft();
			}
			contentDao.setVersion(migrated, nextDraftVersion);
		}

		cloneAllFieldsButMicronode(container, migrated, touchedFields);
		migrateMicronodeFields(ac, container, migrated, fromVersion, toVersion, touchedFields);

		// Ensure the search index is updated accordingly
		sqb.add(contentDao.onUpdated(migrated, branchUuid, DRAFT));
		if (publish) {
			sqb.add(contentDao.onUpdated(migrated, branchUuid, PUBLISHED));
		}
	}

	/**
	 * Migrate the given micronode container.
	 *
	 * @param ac
	 * @param context
	 * @param batch
	 * @param container
	 * @param touchedFields
	 * @param errorsDetected
	 */
	private void migrateMicronodeContainer(NodeMigrationActionContextImpl ac, MicronodeMigrationContext context, EventQueueBatch batch,
										   HibNodeFieldContainer container, Set<String> touchedFields, List<Exception> errorsDetected) {
		String containerUuid = container.getUuid();

		if (log.isDebugEnabled()) {
			log.debug("Migrating container {" + containerUuid + "}");
		}


		try {
			ContentDao contentDao = Tx.get().contentDao();
			HibMicroschemaVersion fromVersion = reloadVersion(context.getFromVersion());
			HibMicroschemaVersion toVersion = reloadVersion(context.getToVersion());
			HibBranch branch = reloadBranch(context.getBranch());
			String branchUuid = branch.getUuid();

			HibNode node = contentDao.getNode(container);
			String languageTag = container.getLanguageTag();
			ac.getNodeParameters().setLanguages(languageTag);
			ac.getVersioningParameters().setVersion("draft");
			HibNodeFieldContainer oldPublished = contentDao.getFieldContainer(node, languageTag, branchUuid, PUBLISHED);

			VersionNumber nextDraftVersion = null;
			// 1. Check whether there is any other published container which we need to handle separately
			if (oldPublished != null && !oldPublished.equals(container)) {
				nextDraftVersion = migratePublishedContainer(ac, batch, branch, node, oldPublished, fromVersion, toVersion, touchedFields);
				nextDraftVersion = nextDraftVersion.nextDraft();
			}

			// 2. Migrate the draft container. This will also update the draft edge.
			migrateDraftContainer(ac, batch, branch, node, container, fromVersion, toVersion, touchedFields, nextDraftVersion);

			postMigrationPurge(container, oldPublished);
		} catch (Exception e1) {
			log.error("Error while handling container {" + containerUuid + "} during schema migration.", e1);
			errorsDetected.add(e1);
		}

	}

	/**
	 * Migrate the published container.
	 *
	 * @param ac
	 * @param sqb           Batch to be used to update the search index
	 * @param branch
	 * @param node          Node of the container
	 * @param container     Container to be migrated
	 * @param fromVersion
	 * @param toVersion
	 * @param touchedFields
	 * @return Version of the new published container
	 * @throws Exception
	 */
	private VersionNumber migratePublishedContainer(NodeMigrationActionContextImpl ac, EventQueueBatch sqb, HibBranch branch, HibNode node,
													HibNodeFieldContainer container, HibMicroschemaVersion fromVersion, HibMicroschemaVersion toVersion,
													Set<String> touchedFields) throws Exception {
		NodeDao nodeDao = Tx.get().nodeDao();
		ContentDao contentDao = Tx.get().contentDao();

		String branchUuid = branch.getUuid();
		ac.getVersioningParameters().setVersion("published");

		HibNodeFieldContainer migrated = contentDao.createEmptyFieldContainer(reloadVersion(container.getSchemaContainerVersion()), node, container.getEditor(), container.getLanguageTag(), branch);
		contentDao.setVersion(migrated, container.getVersion().nextPublished());
		nodeDao.setPublished(node, ac, migrated, branchUuid);
		cloneAllFieldsButMicronode(container, migrated, touchedFields);

		migrateMicronodeFields(ac, container, migrated, fromVersion, toVersion, touchedFields);
		sqb.add(contentDao.onUpdated(migrated, branchUuid, PUBLISHED));
		return migrated.getVersion();

	}

	private void cloneAllFieldsButMicronode(HibNodeFieldContainer oldContainer, HibNodeFieldContainer newContainer, Set<String> touchedFields) {
		FieldSchemaContainer schema = oldContainer.getSchemaContainerVersion().getSchema();
		for (FieldSchema fieldSchema : schema.getFields()) {
			if (!isMicronodeOrMicronodeList(fieldSchema)) {
				HibField field = oldContainer.getField(fieldSchema);
				if (field != null) {
					field.cloneTo(newContainer);
				}
			}
		}
	}

	private boolean isMicronodeOrMicronodeList(FieldSchema fieldSchema) {
		FieldTypes type = FieldTypes.valueByName(fieldSchema.getType());

		if (FieldTypes.MICRONODE.equals(type)) {
			return true;
		}

		if (FieldTypes.LIST.equals(type)) {
			FieldTypes listType = FieldTypes.valueByName(((ListFieldSchema) fieldSchema).getListType());
			return FieldTypes.MICRONODE.equals(listType);
		}

		return false;
	}

	/**
	 * Migrate all micronode fields from old schema version to new schema version
	 *
	 * @param ac            action context
	 * @param oldContainer  old field container
	 * @param newContainer  new field container
	 * @param fromVersion   old schema version
	 * @param toVersion     new schema version
	 * @param touchedFields touched fields
	 * @throws Exception
	 */
	private void migrateMicronodeFields(NodeMigrationActionContextImpl ac, HibNodeFieldContainer oldContainer, HibNodeFieldContainer newContainer,
										HibMicroschemaVersion fromVersion, HibMicroschemaVersion toVersion, Set<String> touchedFields) throws Exception {
		ContentDao contentDao = Tx.get().contentDao();
		// iterate over all fields with micronodes to migrate
		for (HibMicronodeField oldField : contentDao.getMicronodeFields(oldContainer)) {
			HibMicronode oldMicronode = oldField.getMicronode();
			if (oldMicronode.getSchemaContainerVersion().equals(fromVersion)) {
				// clone the micronode (this will not clone its fields)
				HibMicronodeField newMicroField = newContainer.createEmptyMicronode(oldField.getFieldKey(), toVersion);
				HibMicronode newMicronode = newMicroField.getMicronode();

				cloneUntouchedFieldsAndMigrate(ac, oldField, oldMicronode, newMicronode, touchedFields, fromVersion);
			} else {
				// we can clone it safely, since it wasn't affected by the migration
				newContainer.createMicronode(oldField.getFieldKey(), fromVersion);
			}
		}

		// iterate over all micronode list fields to migrate
		for (HibMicronodeFieldList oldListField : contentDao.getMicronodeListFields(oldContainer)) {
			// clone the field (this will not clone the micronodes)
			HibMicronodeFieldList micronodeList = newContainer.createMicronodeList(oldListField.getFieldKey());

			// clone or migrate every micronode
			for (HibMicronodeField oldField : oldListField.getList()) {
				HibMicronode oldMicronode = oldField.getMicronode();

				if (oldMicronode.getSchemaContainerVersion().equals(fromVersion)) {
					HibMicronode newMicronode = micronodeList.createMicronode(toVersion);
					cloneUntouchedFieldsAndMigrate(ac, oldField, oldMicronode, newMicronode, touchedFields, fromVersion);
				} else {
					HibMicronode newMicronode = micronodeList.createMicronode(oldMicronode.getSchemaContainerVersion());
					newMicronode.clone(oldMicronode);
				}
			}
		}
	}

	private void cloneUntouchedFieldsAndMigrate(NodeMigrationActionContextImpl ac, HibMicronodeField oldField, HibMicronode oldMicronode, HibMicronode newMicronode, Set<String> touchedFields, HibMicroschemaVersion fromVersion) throws Exception {
		// clone untouched fields
		for (HibField micronodeField : oldMicronode.getFields()) {
			if (!touchedFields.contains(micronodeField.getFieldKey())) {
				micronodeField.cloneTo(newMicronode);
			}
		}
		// transform to rest and migrate
		MicronodeResponse restModel = oldMicronode.transformToRestSync(ac, 0);
		migrate(ac, newMicronode, restModel, fromVersion);
	}
}
