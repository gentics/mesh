package com.gentics.mesh.core.endpoint.migration.release;

import static com.gentics.mesh.core.data.ContainerType.DRAFT;
import static com.gentics.mesh.core.data.ContainerType.INITIAL;
import static com.gentics.mesh.core.data.ContainerType.PUBLISHED;
import static com.gentics.mesh.core.data.relationship.GraphRelationships.HAS_FIELD_CONTAINER;
import static com.gentics.mesh.core.rest.admin.migration.MigrationStatus.RUNNING;
import static com.gentics.mesh.core.rest.error.Errors.error;
import static io.netty.handler.codec.http.HttpResponseStatus.BAD_REQUEST;

import javax.inject.Inject;
import javax.inject.Singleton;

import com.gentics.mesh.core.data.Project;
import com.gentics.mesh.core.data.Release;
import com.gentics.mesh.core.data.impl.GraphFieldContainerEdgeImpl;
import com.gentics.mesh.core.data.node.Node;
import com.gentics.mesh.core.data.search.SearchQueue;
import com.gentics.mesh.core.data.search.SearchQueueBatch;
import com.gentics.mesh.core.endpoint.migration.AbstractMigrationHandler;
import com.gentics.mesh.core.endpoint.migration.MigrationStatusHandler;
import com.gentics.mesh.core.endpoint.node.BinaryFieldHandler;
import com.gentics.mesh.graphdb.spi.Database;

import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;

@Singleton
public class ReleaseMigrationHandler extends AbstractMigrationHandler {

	private static final Logger log = LoggerFactory.getLogger(ReleaseMigrationHandler.class);

	@Inject
	public ReleaseMigrationHandler(Database db, SearchQueue searchQueue, BinaryFieldHandler nodeFieldAPIHandler) {
		super(db, searchQueue, nodeFieldAPIHandler);
	}

	/**
	 * Migrate all nodes from one release to the other
	 * 
	 * @param newRelease
	 *            new release
	 * @param status
	 */
	public void migrateRelease(Release newRelease, MigrationStatusHandler status) {
		if (newRelease.isMigrated()) {
			throw error(BAD_REQUEST, "Release {" + newRelease.getName() + "} is already migrated");
		}

		Release oldRelease = newRelease.getPreviousRelease();
		if (oldRelease == null) {
			throw error(BAD_REQUEST, "Release {" + newRelease.getName() + "} does not have previous release");
		}

		if (!oldRelease.isMigrated()) {
			throw error(BAD_REQUEST, "Cannot migrate nodes to release {" + newRelease.getName() + "}, because previous release {"
				+ oldRelease.getName() + "} is not fully migrated yet.");
		}

		if (status != null) {
			status.setStatus(RUNNING);
			status.commit();
		}

		long count = 0;
		// Iterate over all nodes of the project and migrate them to the new release
		Project project = oldRelease.getProject();
		SearchQueueBatch sqb = null;
		for (Node node : project.getNodeRoot().findAllIt()) {
			// Create a new SQB to handle the ES update
			if (sqb == null) {
				sqb = searchQueue.create();
			}
			migrateNode(node, sqb, oldRelease, newRelease);
			if (status != null) {
				status.incCompleted();
			}
			if (count % 50 == 0) {
				log.info("Migrated nodes: " + count);
				if (status != null) {
					status.commit();
				}
			}
			count++;
			if (count % 500 == 0) {
				// Process the batch and reset it
				log.info("Syncing batch with size: " + sqb.size());
				sqb.processSync();
				sqb = null;
			}
		}
		if (sqb != null) {
			log.info("Syncing last batch with size: " + sqb.size());
			sqb.processSync();
			sqb = null;
		}

		// TODO track migration errors

		log.info("Migration of " + count + " node done..");
		db.tx(() -> {
			newRelease.setMigrated(true);
		});

	}

	/**
	 * Migrate the node from the old release to the new release. This will effectively create the edges between the new release and the node. Additionally also
	 * the tags will be update to correspond with the new release structure.
	 * 
	 * @param node
	 * @param batch
	 * @param oldRelease
	 * @param newRelease
	 */
	private void migrateNode(Node node, SearchQueueBatch batch, Release oldRelease, Release newRelease) {
		db.tx((tx) -> {
			// Check whether the node already has an initial container and thus was already migrated
			if (node.getGraphFieldContainersIt(newRelease, INITIAL).iterator().hasNext()) {
				return;
			}

			node.getGraphFieldContainersIt(oldRelease, DRAFT).forEach(container -> {
				GraphFieldContainerEdgeImpl initialEdge = node.addFramedEdge(HAS_FIELD_CONTAINER, container, GraphFieldContainerEdgeImpl.class);
				initialEdge.setLanguageTag(container.getLanguage().getLanguageTag());
				initialEdge.setType(INITIAL);
				initialEdge.setReleaseUuid(newRelease.getUuid());

				GraphFieldContainerEdgeImpl draftEdge = node.addFramedEdge(HAS_FIELD_CONTAINER, container, GraphFieldContainerEdgeImpl.class);
				draftEdge.setLanguageTag(container.getLanguage().getLanguageTag());
				draftEdge.setType(DRAFT);
				draftEdge.setReleaseUuid(newRelease.getUuid());
			});
			batch.store(node, newRelease.getUuid(), DRAFT, false);

			node.getGraphFieldContainersIt(oldRelease, PUBLISHED).forEach(container -> {
				GraphFieldContainerEdgeImpl edge = node.addFramedEdge(HAS_FIELD_CONTAINER, container, GraphFieldContainerEdgeImpl.class);
				edge.setLanguageTag(container.getLanguage().getLanguageTag());
				edge.setType(PUBLISHED);
				edge.setReleaseUuid(newRelease.getUuid());
			});
			batch.store(node, newRelease.getUuid(), PUBLISHED, false);

			Node parent = node.getParentNode(oldRelease.getUuid());
			if (parent != null) {
				node.setParentNode(newRelease.getUuid(), parent);
			}

			// migrate tags
			node.getTags(oldRelease).forEach(tag -> node.addTag(tag, newRelease));
		});
	}
}
