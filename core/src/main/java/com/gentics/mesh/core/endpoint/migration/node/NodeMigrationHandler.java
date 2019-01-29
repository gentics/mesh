package com.gentics.mesh.core.endpoint.migration.node;

import static com.gentics.mesh.core.data.ContainerType.DRAFT;
import static com.gentics.mesh.core.data.ContainerType.PUBLISHED;
import static com.gentics.mesh.core.rest.admin.migration.MigrationStatus.COMPLETED;
import static com.gentics.mesh.core.rest.admin.migration.MigrationStatus.RUNNING;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import javax.inject.Inject;
import javax.inject.Singleton;
import javax.validation.constraints.NotNull;

import com.gentics.mesh.context.impl.NodeMigrationActionContextImpl;
import com.gentics.mesh.core.data.Branch;
import com.gentics.mesh.core.data.NodeGraphFieldContainer;
import com.gentics.mesh.core.data.Project;
import com.gentics.mesh.core.data.node.Node;
import com.gentics.mesh.core.data.schema.SchemaContainerVersion;
import com.gentics.mesh.core.data.search.SearchQueue;
import com.gentics.mesh.core.data.search.EventQueueBatch;
import com.gentics.mesh.core.endpoint.migration.AbstractMigrationHandler;
import com.gentics.mesh.core.endpoint.migration.MigrationStatusHandler;
import com.gentics.mesh.core.endpoint.node.BinaryFieldHandler;
import com.gentics.mesh.core.rest.node.NodeResponse;
import com.gentics.mesh.core.rest.node.NodeUpdateRequest;
import com.gentics.mesh.core.rest.schema.SchemaModel;
import com.gentics.mesh.graphdb.spi.Database;
import com.gentics.mesh.util.Tuple;
import com.gentics.mesh.util.VersionNumber;
import com.google.common.collect.Lists;
import com.syncleus.ferma.tx.Tx;

import io.reactivex.Completable;
import io.reactivex.exceptions.CompositeException;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;

/**
 * Handler for node migrations after schema updates.
 */
@Singleton
public class NodeMigrationHandler extends AbstractMigrationHandler {

	private static final Logger log = LoggerFactory.getLogger(NodeMigrationHandler.class);

	@Inject
	public NodeMigrationHandler(Database db, SearchQueue searchQueue, BinaryFieldHandler nodeFieldAPIHandler) {
		super(db, searchQueue, nodeFieldAPIHandler);
	}

	/**
	 * Migrate all nodes of a branch referencing the given schema container to the latest version of the schema.
	 *
	 * @param project
	 *            Specific project to handle
	 * @param branch
	 *            Specific branch to handle
	 * @param fromVersion
	 *            Old container version
	 * @param toVersion
	 *            New container version
	 * @param status
	 *            status handler which will be used to track the progress
	 * @return Completable which is completed once the migration finishes
	 */
	public Completable migrateNodes(NodeMigrationActionContextImpl ac, Project project, Branch branch, SchemaContainerVersion fromVersion,
		SchemaContainerVersion toVersion,
		@NotNull MigrationStatusHandler status) {

		// Prepare the migration - Collect the migration scripts
		List<Tuple<String, List<Tuple<String, Object>>>> migrationScripts = new ArrayList<>();
		Set<String> touchedFields = new HashSet<>();
		SchemaModel newSchema = db.tx(() -> toVersion.getSchema());

		try (Tx tx = db.tx()) {
			prepareMigration(fromVersion, migrationScripts, touchedFields);

			ac.setProject(project);
			ac.setBranch(branch);

			if (status != null) {
				status.setStatus(RUNNING);
				status.commit();
			}
			tx.success();
		} catch (IOException e) {
			log.error("Error while preparing migration");
			return Completable.error(e);
		}

		// Get the draft containers that need to be transformed. Containers which need to be transformed are those which are still linked to older schema
		// versions. We'll work on drafts. The migration code will later on also handle publish versions.
		List<? extends NodeGraphFieldContainer> containers = db.tx(() -> {
			Iterator<? extends NodeGraphFieldContainer> it = fromVersion.getDraftFieldContainers(branch.getUuid());
			return Lists.newArrayList(it);
		});

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

		List<Exception> errorsDetected = migrateLoop(containers, status, (batch, container, errors) ->
			migrateContainer(ac, batch, container, toVersion, migrationScripts, branch, newSchema, errors, touchedFields)
		);

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

	}

	/**
	 * Migrates the given container.
	 * 
	 * @param ac
	 * @param batch
	 * @param container
	 *            Container to be migrated
	 * @param toVersion
	 * @param migrationScripts
	 * @param branch
	 * @param newSchema
	 * @param errorsDetected
	 * @param touchedFields
	 * @return
	 */
	private void migrateContainer(NodeMigrationActionContextImpl ac, EventQueueBatch batch, NodeGraphFieldContainer container,
		SchemaContainerVersion toVersion,
		List<Tuple<String, List<Tuple<String, Object>>>> migrationScripts, Branch branch, SchemaModel newSchema, List<Exception> errorsDetected,
		Set<String> touchedFields) {

		if (log.isDebugEnabled()) {
			log.debug("Migrating container {" + container.getUuid() + "}");
		}
		try {
			Node node = container.getParentNode();
			String languageTag = container.getLanguageTag();
			ac.getNodeParameters().setLanguages(languageTag);
			ac.getVersioningParameters().setVersion("draft");

			VersionNumber nextDraftVersion = null;
			NodeGraphFieldContainer oldPublished = node.getGraphFieldContainer(languageTag, branch.getUuid(), PUBLISHED);
			// 1. Check whether there is any other published container which we need to handle separately
			if (oldPublished != null && !oldPublished.equals(container)) {
				// We only need to migrate the container if the container's schema version is also "old"
				boolean hasSameOldSchemaVersion = container != null
					&& container.getSchemaContainerVersion().id().equals(container.getSchemaContainerVersion().id());
				if (hasSameOldSchemaVersion) {
					nextDraftVersion = migratePublishedContainer(ac, batch, branch, node, oldPublished, toVersion, touchedFields,
						migrationScripts,
						newSchema);
					nextDraftVersion = nextDraftVersion.nextDraft();
				}

			}

			// 2. Migrate the draft container. This will also update the draft edge.
			migrateDraftContainer(ac, batch, branch, node, container, toVersion, touchedFields, migrationScripts, newSchema, nextDraftVersion);
		} catch (Exception e1) {
			log.error("Error while handling container {" + container.getUuid() + "} of node {" + container.getParentNode().getUuid()
				+ "} during schema migration.", e1);
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
	 * @param toVersion
	 * @param touchedFields
	 * @param migrationScripts
	 * @param newSchema
	 *            new schema used to serialize the REST model
	 * @param nextDraftVersion
	 *            Suggested new draft version
	 * @throws Exception
	 */
	private void migrateDraftContainer(NodeMigrationActionContextImpl ac, EventQueueBatch sqb, Branch branch, Node node,
		NodeGraphFieldContainer container, SchemaContainerVersion toVersion, Set<String> touchedFields,
		List<Tuple<String, List<Tuple<String, Object>>>> migrationScripts, SchemaModel newSchema, VersionNumber nextDraftVersion)
		throws Exception {

		String branchUuid = branch.getUuid();
		String languageTag = container.getLanguageTag();

		// Check whether the same container is also used as a published version within the given branch.
		// A migration is always scoped to a specific branch.
		// We need to ensure that the migrated container is also published.
		boolean publish = container.isPublished(branchUuid);

		ac.getVersioningParameters().setVersion(container.getVersion().getFullVersion());
		NodeResponse restModel = node.transformToRestSync(ac, 0, languageTag);

		// Update the schema version. Otherwise deserialisation of the JSON will fail later on.
		restModel.getSchema().setVersion(newSchema.getVersion());

		// Actual migration - Create the new version
		NodeGraphFieldContainer migrated = node.createGraphFieldContainer(container.getLanguageTag(), branch, container.getEditor(), container, true);

		// Ensure that the migrated version is also published since the old version was
		if (publish) {
			migrated.setVersion(container.getVersion().nextPublished());
			node.setPublished(migrated, branchUuid);
		} else {
			if (nextDraftVersion == null) {
				nextDraftVersion = container.getVersion().nextDraft();
			}
			migrated.setVersion(nextDraftVersion);
		}

		// Pass the new version through the migration scripts and update the version
		migrate(ac, migrated, restModel, toVersion, touchedFields, migrationScripts, NodeUpdateRequest.class);

		// Ensure the search index is updated accordingly
		sqb.move(container, migrated, branchUuid, DRAFT);
		if (publish) {
			sqb.move(container, migrated, branchUuid, PUBLISHED);
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
	 * @param toVersion
	 * @param touchedFields
	 * @param migrationScripts
	 * @param newSchema
	 * @return Version of the new published container
	 * @throws Exception
	 */
	private VersionNumber migratePublishedContainer(NodeMigrationActionContextImpl ac, EventQueueBatch sqb, Branch branch, Node node,
		NodeGraphFieldContainer container, SchemaContainerVersion toVersion, Set<String> touchedFields,
		List<Tuple<String, List<Tuple<String, Object>>>> migrationScripts, SchemaModel newSchema) throws Exception {

		String languageTag = container.getLanguageTag();
		String branchUuid = branch.getUuid();

		ac.getVersioningParameters().setVersion("published");
		NodeResponse restModel = node.transformToRestSync(ac, 0, languageTag);
		restModel.getSchema().setVersion(newSchema.getVersion());

		NodeGraphFieldContainer migrated = node.createGraphFieldContainer(container.getLanguageTag(), branch, container.getEditor(), container, true);

		migrated.setVersion(container.getVersion().nextPublished());
		node.setPublished(migrated, branchUuid);

		migrate(ac, migrated, restModel, toVersion, touchedFields, migrationScripts, NodeUpdateRequest.class);
		sqb.store(migrated, branchUuid, PUBLISHED, false);
		return migrated.getVersion();
	}

}
