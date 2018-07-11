package com.gentics.mesh.core.endpoint.migration.node;

import static com.gentics.mesh.core.data.ContainerType.DRAFT;
import static com.gentics.mesh.core.data.ContainerType.PUBLISHED;
import static com.gentics.mesh.core.rest.admin.migration.MigrationStatus.RUNNING;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import javax.inject.Inject;
import javax.inject.Singleton;

import com.gentics.mesh.context.impl.NodeMigrationActionContextImpl;
import com.gentics.mesh.core.data.NodeGraphFieldContainer;
import com.gentics.mesh.core.data.Project;
import com.gentics.mesh.core.data.Release;
import com.gentics.mesh.core.data.node.Node;
import com.gentics.mesh.core.data.schema.SchemaContainerVersion;
import com.gentics.mesh.core.data.search.SearchQueue;
import com.gentics.mesh.core.data.search.SearchQueueBatch;
import com.gentics.mesh.core.endpoint.migration.AbstractMigrationHandler;
import com.gentics.mesh.core.endpoint.migration.MigrationStatusHandler;
import com.gentics.mesh.core.endpoint.node.BinaryFieldHandler;
import com.gentics.mesh.core.rest.node.NodeResponse;
import com.gentics.mesh.core.rest.node.NodeUpdateRequest;
import com.gentics.mesh.core.rest.schema.SchemaModel;
import com.gentics.mesh.graphdb.spi.Database;
import com.gentics.mesh.util.Tuple;
import com.gentics.mesh.util.VersionNumber;
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
	 * Migrate all nodes of a release referencing the given schema container to the latest version of the schema.
	 *
	 * @param project
	 *            Specific project to handle
	 * @param release
	 *            Specific release to handle
	 * @param fromVersion
	 *            Old container version
	 * @param toVersion
	 *            New container version
	 * @param status
	 *            status handler which will be used to track the progress
	 * @return Completable which is completed once the migration finishes
	 */
	public Completable migrateNodes(NodeMigrationActionContextImpl ac, Project project, Release release, SchemaContainerVersion fromVersion, SchemaContainerVersion toVersion,
		MigrationStatusHandler status) {

		// Get the draft containers that need to be transformed. Containers which need to be transformed are those which are still linked to older schema
		// versions. We'll work on drafts. The migration code will later on also handle publish versions.
		Iterator<? extends NodeGraphFieldContainer> fieldContainers = fromVersion.getDraftFieldContainers(release.getUuid());

		// Prepare the migration - Collect the migration scripts
		List<Tuple<String, List<Tuple<String, Object>>>> migrationScripts = new ArrayList<>();
		Set<String> touchedFields = new HashSet<>();
		try (Tx tx = db.tx()) {
			prepareMigration(fromVersion, migrationScripts, touchedFields);
		} catch (IOException e) {
			return Completable.error(e);
		}

		ac.setProject(project);
		ac.setRelease(release);

		SchemaModel newSchema = toVersion.getSchema();

		if (status != null) {
			status.setStatus(RUNNING);
			status.commit();
		}

		// Iterate over all containers and invoke a migration for each one
		long count = 0;
		List<Exception> errorsDetected = new ArrayList<>();
		SearchQueueBatch sqb = null;
		while (fieldContainers.hasNext()) {
			NodeGraphFieldContainer container = fieldContainers.next();
			// Create a new SQB to handle the ES update
			if (sqb == null) {
				sqb = searchQueue.create();
			}
			migrateContainer(ac, sqb, container, toVersion, migrationScripts, release, newSchema, errorsDetected, touchedFields);

			if (status != null) {
				status.incCompleted();
			}
			if (count % 50 == 0) {
				log.info("Migrated containers: " + count);
				if (status != null) {
					status.commit();
				}
			}
			if (count % 500 == 0) {
				// Process the batch and reset it
				log.info("Syncing batch with size: " + sqb.size());
				sqb.processSync();
				sqb = null;
			}
			count++;
		}
		if (sqb != null) {
			log.info("Syncing last batch with size: " + sqb.size());
			sqb.processSync();
			sqb = null;
		}

		log.info("Migration of " + count + " containers done..");
		log.info("Encountered {" + errorsDetected.size() + "} errors during node migration.");
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
	 * @param release
	 * @param newSchema
	 * @param errorsDetected
	 * @param touchedFields
	 * @return
	 */
	private void migrateContainer(NodeMigrationActionContextImpl ac, SearchQueueBatch batch, NodeGraphFieldContainer container,
		SchemaContainerVersion toVersion,
		List<Tuple<String, List<Tuple<String, Object>>>> migrationScripts, Release release, SchemaModel newSchema, List<Exception> errorsDetected,
		Set<String> touchedFields) {

		if (log.isDebugEnabled()) {
			log.debug("Migrating container {" + container.getUuid() + "}");
		}
		try {
			// Run the actual migration in a dedicated transaction
			db.tx((tx) -> {

				Node node = container.getParentNode();
				String languageTag = container.getLanguage().getLanguageTag();
				ac.getNodeParameters().setLanguages(languageTag);
				ac.getVersioningParameters().setVersion("draft");

				VersionNumber nextDraftVersion = null;
				NodeGraphFieldContainer oldPublished = node.getGraphFieldContainer(languageTag, release.getUuid(), PUBLISHED);
				// 1. Check whether there is any other published container which we need to handle separately
				if (oldPublished != null && !oldPublished.equals(container)) {
					// We only need to migrate the container if the container's schema version is also "old"
					boolean hasSameOldSchemaVersion = container != null
						&& container.getSchemaContainerVersion().getId().equals(container.getSchemaContainerVersion().getId());
					if (hasSameOldSchemaVersion) {
						nextDraftVersion = migratePublishedContainer(ac, batch, release, node, oldPublished, toVersion, touchedFields,
							migrationScripts,
							newSchema);
						nextDraftVersion = nextDraftVersion.nextDraft();
					}

				}

				// 2. Migrate the draft container. This will also update the draft edge.
				migrateDraftContainer(ac, batch, release, node, container, toVersion, touchedFields, migrationScripts, newSchema,
					nextDraftVersion);
			});
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
	 * @param release
	 *            Release in which the migration is running
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
	private void migrateDraftContainer(NodeMigrationActionContextImpl ac, SearchQueueBatch sqb, Release release, Node node,
		NodeGraphFieldContainer container, SchemaContainerVersion toVersion, Set<String> touchedFields,
		List<Tuple<String, List<Tuple<String, Object>>>> migrationScripts, SchemaModel newSchema, VersionNumber nextDraftVersion)
		throws Exception {

		String releaseUuid = release.getUuid();
		String languageTag = container.getLanguage().getLanguageTag();

		// Check whether the same container is also used as a published version within the given release.
		// A migration is always scoped to a specific release.
		// We need to ensure that the migrated container is also published.
		boolean publish = container.isPublished(releaseUuid);

		ac.getVersioningParameters().setVersion(container.getVersion().getFullVersion());
		NodeResponse restModel = node.transformToRestSync(ac, 0, languageTag);

		// Update the schema version. Otherwise deserialisation of the JSON will fail later on.
		restModel.getSchema().setVersion(newSchema.getVersion());

		// Actual migration - Create the new version
		NodeGraphFieldContainer migrated = node.createGraphFieldContainer(container.getLanguage(), release, container.getEditor(), container, true);

		// Ensure that the migrated version is also published since the old version was
		if (publish) {
			migrated.setVersion(container.getVersion().nextPublished());
			node.setPublished(migrated, releaseUuid);
		} else {
			if (nextDraftVersion == null) {
				nextDraftVersion = container.getVersion().nextDraft();
			}
			migrated.setVersion(nextDraftVersion);
		}

		// Pass the new version through the migration scripts and update the version
		migrate(ac, migrated, restModel, toVersion, touchedFields, migrationScripts, NodeUpdateRequest.class);

		// Ensure the search index is updated accordingly
		sqb.move(container, migrated, releaseUuid, DRAFT);
		if (publish) {
			sqb.move(container, migrated, releaseUuid, PUBLISHED);
		}
	}

	/**
	 * Migrate the published container.
	 * 
	 * @param ac
	 * @param sqb
	 *            Batch to be used to update the search index
	 * @param release
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
	private VersionNumber migratePublishedContainer(NodeMigrationActionContextImpl ac, SearchQueueBatch sqb, Release release, Node node,
		NodeGraphFieldContainer container, SchemaContainerVersion toVersion, Set<String> touchedFields,
		List<Tuple<String, List<Tuple<String, Object>>>> migrationScripts, SchemaModel newSchema) throws Exception {

		String languageTag = container.getLanguage().getLanguageTag();
		String releaseUuid = release.getUuid();

		ac.getVersioningParameters().setVersion("published");
		NodeResponse restModel = node.transformToRestSync(ac, 0, languageTag);
		restModel.getSchema().setVersion(newSchema.getVersion());

		NodeGraphFieldContainer migrated = node.createGraphFieldContainer(container.getLanguage(), release, container.getEditor(), container, true);

		migrated.setVersion(container.getVersion().nextPublished());
		node.setPublished(migrated, releaseUuid);

		migrate(ac, migrated, restModel, toVersion, touchedFields, migrationScripts, NodeUpdateRequest.class);
		sqb.store(migrated, releaseUuid, PUBLISHED, false);
		return migrated.getVersion();
	}

}
