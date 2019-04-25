package com.gentics.mesh.core.endpoint.migration.micronode;

import static com.gentics.mesh.core.rest.common.ContainerType.DRAFT;
import static com.gentics.mesh.core.rest.common.ContainerType.PUBLISHED;
import static com.gentics.mesh.core.rest.job.JobStatus.COMPLETED;
import static com.gentics.mesh.core.rest.job.JobStatus.RUNNING;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.inject.Inject;
import javax.inject.Singleton;

import com.gentics.mesh.context.MicronodeMigrationContext;
import com.gentics.mesh.context.impl.NodeMigrationActionContextImpl;
import com.gentics.mesh.core.data.Branch;
import com.gentics.mesh.core.data.NodeGraphFieldContainer;
import com.gentics.mesh.core.data.node.Micronode;
import com.gentics.mesh.core.data.node.Node;
import com.gentics.mesh.core.data.node.field.list.MicronodeGraphFieldList;
import com.gentics.mesh.core.data.node.field.nesting.MicronodeGraphField;
import com.gentics.mesh.core.data.schema.MicroschemaContainerVersion;
import com.gentics.mesh.core.endpoint.migration.AbstractMigrationHandler;
import com.gentics.mesh.core.endpoint.migration.MigrationStatusHandler;
import com.gentics.mesh.core.endpoint.node.BinaryUploadHandler;
import com.gentics.mesh.core.rest.event.node.MicroschemaMigrationCause;
import com.gentics.mesh.core.rest.micronode.MicronodeResponse;
import com.gentics.mesh.event.EventQueueBatch;
import com.gentics.mesh.graphdb.spi.Database;
import com.gentics.mesh.metric.MetricsService;
import com.gentics.mesh.util.Tuple;
import com.gentics.mesh.util.VersionNumber;

import io.reactivex.Completable;
import io.reactivex.exceptions.CompositeException;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;

@Singleton
public class MicronodeMigrationHandler extends AbstractMigrationHandler {

	private static final Logger log = LoggerFactory.getLogger(MicronodeMigrationHandler.class);

	@Inject
	public MicronodeMigrationHandler(Database db, BinaryUploadHandler binaryFieldHandler, MetricsService metrics) {
		super(db, binaryFieldHandler, metrics);
	}

	/**
	 * Migrate all micronodes referencing the given microschema container to the latest version
	 * 
	 * @param context
	 * @return Completable which will be completed once the migration has completed
	 */
	public Completable migrateMicronodes(MicronodeMigrationContext context) {
		context.validate();
		return Completable.defer(() -> {
			Branch branch = context.getBranch();
			MicroschemaContainerVersion fromVersion = context.getFromVersion();
			MicroschemaContainerVersion toVersion = context.getToVersion();
			MigrationStatusHandler status = context.getStatus();
			MicroschemaMigrationCause cause = context.getCause();

			// Collect the migration scripts
			NodeMigrationActionContextImpl ac = new NodeMigrationActionContextImpl();
			List<Tuple<String, List<Tuple<String, Object>>>> migrationScripts = new ArrayList<>();
			Set<String> touchedFields = new HashSet<>();
			try {
				db.tx(() -> {
					prepareMigration(fromVersion, migrationScripts, touchedFields);

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
			List<? extends NodeGraphFieldContainer> fieldContainersResult = db.tx(() -> {
				return fromVersion.getDraftFieldContainers(branch.getUuid()).list();
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
				(batch, container, errors) -> migrateMicronodeContainer(ac,
					batch, branch, fromVersion, toVersion, container, touchedFields, migrationScripts, errors));

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
	 * @param migrationScripts
	 * @param nextDraftVersion
	 * @throws Exception
	 */
	private void migrateDraftContainer(NodeMigrationActionContextImpl ac, EventQueueBatch sqb, Branch branch, Node node,
		NodeGraphFieldContainer container, MicroschemaContainerVersion fromVersion, MicroschemaContainerVersion toVersion,
		Set<String> touchedFields, List<Tuple<String, List<Tuple<String, Object>>>> migrationScripts, VersionNumber nextDraftVersion)
		throws Exception {

		String branchUuid = branch.getUuid();
		ac.getVersioningParameters().setVersion(container.getVersion().getFullVersion());

		boolean publish = container.isPublished(branchUuid);

		// Clone the field container. This will also update the draft edge
		NodeGraphFieldContainer migrated = node.createGraphFieldContainer(container.getLanguageTag(), branch, container.getEditor(), container, true);
		if (publish) {
			migrated.setVersion(container.getVersion().nextPublished());
			// Ensure that the publish edge is also updated correctly
			node.setPublished(migrated, branchUuid);
		} else {
			if (nextDraftVersion == null) {
				nextDraftVersion = container.getVersion().nextDraft();
			}
			migrated.setVersion(nextDraftVersion);
		}

		migrateMicronodeFields(ac, migrated, fromVersion, toVersion, touchedFields, migrationScripts);

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
	 * @param migrationScripts
	 * @param errorsDetected
	 */
	private void migrateMicronodeContainer(NodeMigrationActionContextImpl ac, EventQueueBatch batch, Branch branch,
		MicroschemaContainerVersion fromVersion,
		MicroschemaContainerVersion toVersion, NodeGraphFieldContainer container, Set<String> touchedFields,
		List<Tuple<String, List<Tuple<String, Object>>>> migrationScripts, List<Exception> errorsDetected) {

		if (log.isDebugEnabled()) {
			log.debug("Migrating container {" + container.getUuid() + "}");
		}
		String branchUuid = branch.getUuid();

		// Run the actual migration in a dedicated transaction
		try {
			db.tx((tx) -> {

				Node node = container.getParentNode();
				String languageTag = container.getLanguageTag();
				ac.getNodeParameters().setLanguages(languageTag);
				ac.getVersioningParameters().setVersion("draft");
				NodeGraphFieldContainer oldPublished = node.getGraphFieldContainer(languageTag, branchUuid, PUBLISHED);

				VersionNumber nextDraftVersion = null;
				// 1. Check whether there is any other published container which we need to handle separately
				if (oldPublished != null && !oldPublished.equals(container)) {
					nextDraftVersion = migratePublishedContainer(ac, batch, branch, node, container, fromVersion, toVersion, touchedFields,
						migrationScripts);
					nextDraftVersion = nextDraftVersion.nextDraft();
				}

				// 2. Migrate the draft container. This will also update the draft edge.
				migrateDraftContainer(ac, batch, branch, node, container, fromVersion, toVersion, touchedFields, migrationScripts, nextDraftVersion);
			});
		} catch (Exception e1) {
			log.error("Error while handling container {" + container.getUuid() + "} during schema migration.", e1);
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
	 * @param migrationScripts
	 * @return Version of the new published container
	 * @throws Exception
	 */
	private VersionNumber migratePublishedContainer(NodeMigrationActionContextImpl ac, EventQueueBatch sqb, Branch branch, Node node,
		NodeGraphFieldContainer container, MicroschemaContainerVersion fromVersion, MicroschemaContainerVersion toVersion,
		Set<String> touchedFields, List<Tuple<String, List<Tuple<String, Object>>>> migrationScripts) throws Exception {

		String branchUuid = branch.getUuid();
		ac.getVersioningParameters().setVersion("published");

		NodeGraphFieldContainer migrated = node.createGraphFieldContainer(container.getLanguageTag(), branch, container.getEditor(), container, true);
		migrated.setVersion(container.getVersion().nextPublished());
		node.setPublished(migrated, branchUuid);

		migrateMicronodeFields(ac, migrated, fromVersion, toVersion, touchedFields, migrationScripts);
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
	 * @param migrationScripts
	 *            migration scripts
	 * @throws Exception
	 */
	protected void migrateMicronodeFields(NodeMigrationActionContextImpl ac, NodeGraphFieldContainer container,
		MicroschemaContainerVersion fromVersion, MicroschemaContainerVersion toVersion, Set<String> touchedFields,
		List<Tuple<String, List<Tuple<String, Object>>>> migrationScripts) throws Exception {
		// iterate over all fields with micronodes to migrate
		for (MicronodeGraphField field : container.getMicronodeFields(fromVersion)) {
			// clone the field (this will clone the micronode)
			field = container.createMicronode(field.getFieldKey(), fromVersion);
			Micronode micronode = field.getMicronode();
			// transform to rest and migrate
			MicronodeResponse restModel = micronode.transformToRestSync(ac, 0);
			migrate(ac, micronode, restModel, toVersion, touchedFields, migrationScripts, MicronodeResponse.class);
		}

		// iterate over all micronode list fields to migrate
		for (MicronodeGraphFieldList field : container.getMicronodeListFields(fromVersion)) {
			MicronodeGraphFieldList oldListField = field;

			// clone the field (this will not clone the micronodes)
			field = container.createMicronodeFieldList(field.getFieldKey());

			// clone every micronode
			for (MicronodeGraphField oldField : oldListField.getList()) {
				Micronode oldMicronode = oldField.getMicronode();
				Micronode newMicronode = field.createMicronode();
				newMicronode.setSchemaContainerVersion(oldMicronode.getSchemaContainerVersion());
				newMicronode.clone(oldMicronode);

				// migrate the micronode, if it uses the fromVersion
				if (newMicronode.getSchemaContainerVersion().equals(fromVersion)) {
					// transform to rest and migrate
					MicronodeResponse restModel = newMicronode.transformToRestSync(ac, 0);
					migrate(ac, newMicronode, restModel, toVersion, touchedFields, migrationScripts, MicronodeResponse.class);
				}
			}
		}
	}

}
