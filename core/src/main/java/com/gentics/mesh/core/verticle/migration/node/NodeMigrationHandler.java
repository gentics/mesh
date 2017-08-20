package com.gentics.mesh.core.verticle.migration.node;

import static com.gentics.mesh.core.data.ContainerType.DRAFT;
import static com.gentics.mesh.core.data.ContainerType.INITIAL;
import static com.gentics.mesh.core.data.ContainerType.PUBLISHED;
import static com.gentics.mesh.core.data.relationship.GraphRelationships.HAS_FIELD_CONTAINER;
import static com.gentics.mesh.core.rest.error.Errors.error;
import static io.netty.handler.codec.http.HttpResponseStatus.BAD_REQUEST;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.inject.Inject;
import javax.inject.Singleton;

import com.gentics.mesh.context.impl.NodeMigrationActionContextImpl;
import com.gentics.mesh.core.data.NodeGraphFieldContainer;
import com.gentics.mesh.core.data.Project;
import com.gentics.mesh.core.data.Release;
import com.gentics.mesh.core.data.impl.GraphFieldContainerEdgeImpl;
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

import io.vertx.core.json.JsonObject;
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

	public JsonObject createInfoJson() {
		return null;
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
		List<? extends NodeGraphFieldContainer> fieldContainers = db.tx(() -> fromVersion.getFieldContainers(release.getUuid()));

		// No field containers -> no nodes, migration is done
		if (fieldContainers.isEmpty()) {
			return Completable.complete();
		} else {
			log.info("Found {" + fieldContainers.size() + "} containers which still make use of schema {" + fromVersion.getName() + "@"
					+ fromVersion.getVersion() + "}");
		}

		if (status != null) {
			status.setTotalElements(fieldContainers.size());
		}

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
		List<Completable> batches = new ArrayList<>();
		List<Exception> errorsDetected = new ArrayList<>();

		// Iterate over all containers and invoke a migration for each one
		for (NodeGraphFieldContainer container : fieldContainers) {
			SearchQueueBatch batch = migrateContainer(ac, container, toVersion, migrationScripts, release, newSchema, errorsDetected, touchedFields);
			// Process the search queue batch in order to update the search index
			if (batch != null) {
				batches.add(batch.processAsync());
			}

			if (status != null) {
				status.incDoneElements();
			}
		}

		Completable result = Completable.complete();
		if (!errorsDetected.isEmpty()) {
			result = Completable.error(new CompositeException(errorsDetected));
		}

		return Completable.merge(batches).andThen(result);
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
	private SearchQueueBatch migrateContainer(NodeMigrationActionContextImpl ac, NodeGraphFieldContainer container, SchemaContainerVersion toVersion,
			List<Tuple<String, List<Tuple<String, Object>>>> migrationScripts, Release release, SchemaModel newSchema, List<Exception> errorsDetected,
			Set<String> touchedFields) {
		SearchQueueBatch batch = db.tx((tx) -> {
			String releaseUuid = release.getUuid();
			SearchQueueBatch sqb = searchQueue.create();

			try {
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
				sqb.store(node, releaseUuid, DRAFT, false);
				if (publish) {
					sqb.store(node, releaseUuid, PUBLISHED, false);
				}
				tx.success();
				return sqb;
			} catch (Exception e1) {
				log.error("Error while handling container {" + container.getUuid() + "} during schema migration.", e1);
				errorsDetected.add(e1);
				tx.failure();
				return null;
			}
		});
		return batch;

	}

	/**
	 * Migrate all nodes from one release to the other
	 * 
	 * @param newRelease
	 *            new release
	 * @return Completable which will be invoked once the migration has completed
	 */
	public Completable migrateNodes(Release newRelease) {
		Release oldRelease = db.tx(() -> {
			if (newRelease.isMigrated()) {
				throw error(BAD_REQUEST, "Release {" + newRelease.getName() + "} is already migrated");
			}

			Release old = newRelease.getPreviousRelease();
			if (old == null) {
				throw error(BAD_REQUEST, "Release {" + newRelease.getName() + "} does not have previous release");
			}

			if (!old.isMigrated()) {
				throw error(BAD_REQUEST, "Cannot migrate nodes to release {" + newRelease.getName() + "}, because previous release {" + old.getName()
						+ "} is not fully migrated yet.");
			}

			return old;
		});

		String oldReleaseUuid = db.tx(() -> oldRelease.getUuid());
		String newReleaseUuid = db.tx(() -> newRelease.getUuid());
		List<? extends Node> nodes = db.tx(() -> oldRelease.getRoot().getProject().getNodeRoot().findAll());
		List<Completable> batches = new ArrayList<>();
		for (Node node : nodes) {
			SearchQueueBatch sqb = db.tx(() -> {
				if (!node.getGraphFieldContainers(newRelease, INITIAL).isEmpty()) {
					return null;
				}
				node.getGraphFieldContainers(oldRelease, DRAFT).stream().forEach(container -> {
					GraphFieldContainerEdgeImpl initialEdge = node.addFramedEdge(HAS_FIELD_CONTAINER, container, GraphFieldContainerEdgeImpl.class);
					initialEdge.setLanguageTag(container.getLanguage().getLanguageTag());
					initialEdge.setType(INITIAL);
					initialEdge.setReleaseUuid(newReleaseUuid);

					GraphFieldContainerEdgeImpl draftEdge = node.addFramedEdge(HAS_FIELD_CONTAINER, container, GraphFieldContainerEdgeImpl.class);
					draftEdge.setLanguageTag(container.getLanguage().getLanguageTag());
					draftEdge.setType(DRAFT);
					draftEdge.setReleaseUuid(newReleaseUuid);
				});
				SearchQueueBatch batch = searchQueue.create();
				batch.store(node, newReleaseUuid, DRAFT, false);

				node.getGraphFieldContainers(oldRelease, PUBLISHED).stream().forEach(container -> {
					GraphFieldContainerEdgeImpl edge = node.addFramedEdge(HAS_FIELD_CONTAINER, container, GraphFieldContainerEdgeImpl.class);
					edge.setLanguageTag(container.getLanguage().getLanguageTag());
					edge.setType(PUBLISHED);
					edge.setReleaseUuid(newReleaseUuid);
				});
				batch.store(node, newReleaseUuid, PUBLISHED, false);

				Node parent = node.getParentNode(oldReleaseUuid);
				if (parent != null) {
					node.setParentNode(newReleaseUuid, parent);
				}

				// migrate tags
				node.getTags(oldRelease).forEach(tag -> node.addTag(tag, newRelease));
				return batch;
			});
			batches.add(sqb.processAsync());
		}

		db.tx(() -> {
			newRelease.setMigrated(true);
			return null;
		});

		return Completable.merge(batches);
	}

}
