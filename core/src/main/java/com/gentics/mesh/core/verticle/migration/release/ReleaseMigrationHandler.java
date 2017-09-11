package com.gentics.mesh.core.verticle.migration.release;

import static com.gentics.mesh.core.data.ContainerType.DRAFT;
import static com.gentics.mesh.core.data.ContainerType.INITIAL;
import static com.gentics.mesh.core.data.ContainerType.PUBLISHED;
import static com.gentics.mesh.core.data.relationship.GraphRelationships.HAS_FIELD_CONTAINER;
import static com.gentics.mesh.core.rest.admin.migration.MigrationStatus.RUNNING;
import static com.gentics.mesh.core.rest.error.Errors.error;
import static io.netty.handler.codec.http.HttpResponseStatus.BAD_REQUEST;

import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;
import javax.inject.Singleton;

import com.gentics.mesh.core.data.Project;
import com.gentics.mesh.core.data.Release;
import com.gentics.mesh.core.data.impl.GraphFieldContainerEdgeImpl;
import com.gentics.mesh.core.data.node.Node;
import com.gentics.mesh.core.data.schema.SchemaContainerVersion;
import com.gentics.mesh.core.data.search.SearchQueue;
import com.gentics.mesh.core.data.search.SearchQueueBatch;
import com.gentics.mesh.core.rest.schema.SchemaModel;
import com.gentics.mesh.core.verticle.migration.AbstractMigrationHandler;
import com.gentics.mesh.core.verticle.migration.MigrationStatusHandler;
import com.gentics.mesh.core.verticle.node.BinaryFieldHandler;
import com.gentics.mesh.graphdb.spi.Database;

import rx.Completable;

@Singleton
public class ReleaseMigrationHandler extends AbstractMigrationHandler {

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
	 * @return Completable which will be invoked once the migration has completed.
	 */
	public Completable migrateRelease(Release newRelease, MigrationStatusHandler status) {
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

		String newReleaseUuid = newRelease.getUuid();
		Project project = oldRelease.getProject();
		List<Completable> batches = new ArrayList<>();

		if (status != null) {
			status.getInfo().setStatus(RUNNING);
			status.updateStatus();
		}

		// Add the needed indices and mappings
		SearchQueueBatch indexCreationBatch = searchQueue.create();
		for (SchemaContainerVersion schemaVersion : newRelease.findAllSchemaVersions()) {
			SchemaModel schema = schemaVersion.getSchema();
			indexCreationBatch.createNodeIndex(project.getUuid(), newReleaseUuid, schemaVersion.getUuid(), PUBLISHED, schema);
			indexCreationBatch.createNodeIndex(project.getUuid(), newReleaseUuid, schemaVersion.getUuid(), DRAFT, schema);
		}

		// // Migrate each node individually
		List<? extends Node> nodes = project.getNodeRoot().findAll();
		for (Node node : nodes) {
			SearchQueueBatch sqb = db.tx(() -> {
				return migrateNode(node, oldRelease, newRelease);
			});
			batches.add(sqb.processAsync());
		}

		db.tx(() -> {
			newRelease.setMigrated(true);
		});

		return indexCreationBatch.processAsync().andThen(Completable.merge(batches));
	}

	/**
	 * Migrate the node from the old release to the new release. This will effectively create the edges between the new release and the node. Additionally also
	 * the tags will be update to correspond with the new release structure.
	 * 
	 * @param node
	 * @param oldRelease
	 * @param newRelease
	 * @return
	 */
	private SearchQueueBatch migrateNode(Node node, Release oldRelease, Release newRelease) {
		if (!node.getGraphFieldContainers(newRelease, INITIAL).isEmpty()) {
			return null;
		}
		node.getGraphFieldContainers(oldRelease, DRAFT).stream().forEach(container -> {
			GraphFieldContainerEdgeImpl initialEdge = node.addFramedEdge(HAS_FIELD_CONTAINER, container, GraphFieldContainerEdgeImpl.class);
			initialEdge.setLanguageTag(container.getLanguage().getLanguageTag());
			initialEdge.setType(INITIAL);
			initialEdge.setReleaseUuid(newRelease.getUuid());

			GraphFieldContainerEdgeImpl draftEdge = node.addFramedEdge(HAS_FIELD_CONTAINER, container, GraphFieldContainerEdgeImpl.class);
			draftEdge.setLanguageTag(container.getLanguage().getLanguageTag());
			draftEdge.setType(DRAFT);
			draftEdge.setReleaseUuid(newRelease.getUuid());
		});
		SearchQueueBatch batch = searchQueue.create();
		batch.store(node, newRelease.getUuid(), DRAFT, false);

		node.getGraphFieldContainers(oldRelease, PUBLISHED).stream().forEach(container -> {
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
		return batch;

	}
}
