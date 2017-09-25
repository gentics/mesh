package com.gentics.mesh.core.verticle.migration.node;

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
import com.gentics.mesh.core.rest.node.NodeResponse;
import com.gentics.mesh.core.rest.node.NodeUpdateRequest;
import com.gentics.mesh.core.rest.schema.SchemaModel;
import com.gentics.mesh.core.verticle.migration.AbstractMigrationHandler;
import com.gentics.mesh.core.verticle.migration.MigrationStatusHandler;
import com.gentics.mesh.core.verticle.node.BinaryFieldHandler;
import com.gentics.mesh.graphdb.spi.Database;
import com.gentics.mesh.util.Tuple;
import com.syncleus.ferma.tx.Tx;

import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import rx.Completable;
import rx.exceptions.CompositeException;

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
	public Completable migrateNodes(Project project, Release release, SchemaContainerVersion fromVersion, SchemaContainerVersion toVersion,
			MigrationStatusHandler status) {

		// Get the containers of nodes, that need to be transformed. Containers which need to be transformed are those which are still linked to older schema
		// versions.
		Iterator<NodeGraphFieldContainer> fieldContainers = fromVersion.getFieldContainers(release.getUuid());

		// Prepare the migration - Collect the migration scripts
		List<Tuple<String, List<Tuple<String, Object>>>> migrationScripts = new ArrayList<>();
		Set<String> touchedFields = new HashSet<>();
		try (Tx tx = db.tx()) {
			prepareMigration(fromVersion, migrationScripts, touchedFields);
		} catch (IOException e) {
			return Completable.error(e);
		}

		NodeMigrationActionContextImpl ac = new NodeMigrationActionContextImpl();
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
		while (fieldContainers.hasNext()) {
			NodeGraphFieldContainer container = fieldContainers.next();
			migrateContainer(ac, container, toVersion, migrationScripts, release, newSchema, errorsDetected, touchedFields);

			if (status != null) {
				status.incCompleted();
			}
			if (count % 50 == 0) {
				log.info("Migrated containers: " + count);
				if (status != null) {
					status.commit();
				}
			}
			count++;
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
	private void migrateContainer(NodeMigrationActionContextImpl ac, NodeGraphFieldContainer container, SchemaContainerVersion toVersion,
			List<Tuple<String, List<Tuple<String, Object>>>> migrationScripts, Release release, SchemaModel newSchema, List<Exception> errorsDetected,
			Set<String> touchedFields) {

		if (log.isDebugEnabled()) {
			log.debug("Migrating container {" + container.getUuid() + "}");
		}
		String releaseUuid = release.getUuid();

		// Run the actual migration in a dedicated transaction
		try {
			SearchQueueBatch batch = db.tx((tx) -> {
				SearchQueueBatch sqb = searchQueue.create();

				Node node = container.getParentNode();
				String languageTag = container.getLanguage().getLanguageTag();
				ac.getNodeParameters().setLanguages(languageTag);
				ac.getVersioningParameters().setVersion("draft");

				// Check whether the container is published in the given release. A migration is always scoped to a specific release.
				boolean publish = false;
				if (container.isPublished(releaseUuid)) {
					publish = true;
				} else {
					// Check whether there is any other published container
					NodeGraphFieldContainer oldPublished = node.getGraphFieldContainer(languageTag, releaseUuid, PUBLISHED);

					// We only need to migrate the container if the container's schema version is also "old"
					boolean hasSameOldSchemaVersion = oldPublished != null
							&& oldPublished.getSchemaContainerVersion().getId().equals(container.getSchemaContainerVersion().getId());
					if (oldPublished != null && hasSameOldSchemaVersion) {
						ac.getVersioningParameters().setVersion("published");
						NodeResponse restModel = node.transformToRestSync(ac, 0, languageTag);
						restModel.getSchema().setVersion(newSchema.getVersion());

						NodeGraphFieldContainer migrated = node.createGraphFieldContainer(oldPublished.getLanguage(), release,
								oldPublished.getEditor(), oldPublished);
						migrated.setVersion(oldPublished.getVersion().nextPublished());
						node.setPublished(migrated, releaseUuid);
						migrate(ac, migrated, restModel, toVersion, touchedFields, migrationScripts, NodeUpdateRequest.class);
						sqb.store(migrated, releaseUuid, PUBLISHED, false);

						ac.getVersioningParameters().setVersion("draft");
					}
				}

				NodeResponse restModel = node.transformToRestSync(ac, 0, languageTag);

				// Update the schema version. Otherwise deserialisation of the JSON will fail later on.
				restModel.getSchema().setVersion(newSchema.getVersion());

				// Actual migration - Create the new version
				NodeGraphFieldContainer migrated = node.createGraphFieldContainer(container.getLanguage(), release, container.getEditor(), container);

				// Ensure that the migrated version is also published since the old version was
				if (publish) {
					migrated.setVersion(container.getVersion().nextPublished());
					node.setPublished(migrated, releaseUuid);
				}

				// Pass the new version through the migration scripts and update the version
				migrate(ac, migrated, restModel, toVersion, touchedFields, migrationScripts, NodeUpdateRequest.class);

				// Ensure the search index is updated accordingly
				sqb.move(container, migrated, releaseUuid, DRAFT);
				if (publish) {
					sqb.move(container, migrated, releaseUuid, PUBLISHED);
				}
				return sqb;
			});
			// Process the search queue batch in order to update the search index
			if (batch != null) {
				batch.processSync();
			}
		} catch (Exception e1) {
			log.error("Error while handling container {" + container.getUuid() + "} during schema migration.", e1);
			errorsDetected.add(e1);
		}

	}

}
