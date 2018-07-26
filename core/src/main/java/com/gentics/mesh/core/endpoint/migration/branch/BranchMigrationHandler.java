package com.gentics.mesh.core.endpoint.migration.branch;

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
import com.gentics.mesh.core.data.Branch;
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
public class BranchMigrationHandler extends AbstractMigrationHandler {

	private static final Logger log = LoggerFactory.getLogger(BranchMigrationHandler.class);

	@Inject
	public BranchMigrationHandler(Database db, SearchQueue searchQueue, BinaryFieldHandler nodeFieldAPIHandler) {
		super(db, searchQueue, nodeFieldAPIHandler);
	}

	/**
	 * Migrate all nodes from one branch to the other
	 * 
	 * @param newBranch
	 *            new branch
	 * @param status
	 */
	public void migrateBranch(Branch newBranch, MigrationStatusHandler status) {
		if (newBranch.isMigrated()) {
			throw error(BAD_REQUEST, "Branch {" + newBranch.getName() + "} is already migrated");
		}

		Branch oldBranch = newBranch.getPreviousBranch();
		if (oldBranch == null) {
			throw error(BAD_REQUEST, "Branch {" + newBranch.getName() + "} does not have previous branch");
		}

		if (!oldBranch.isMigrated()) {
			throw error(BAD_REQUEST, "Cannot migrate nodes to branch {" + newBranch.getName() + "}, because previous branch {"
				+ oldBranch.getName() + "} is not fully migrated yet.");
		}

		if (status != null) {
			status.setStatus(RUNNING);
			status.commit();
		}

		long count = 0;
		// Iterate over all nodes of the project and migrate them to the new branch
		Project project = oldBranch.getProject();
		for (Node node : project.getNodeRoot().findAllIt()) {
			SearchQueueBatch sqb = db.tx(() -> {
				return migrateNode(node, oldBranch, newBranch);
			});
			if (sqb != null) {
				sqb.processSync();
			}
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
		}

		// TODO track migration errors

		log.info("Migration of " + count + " node done..");
		db.tx(() -> {
			newBranch.setMigrated(true);
		});

	}

	/**
	 * Migrate the node from the old branch to the new branch. This will effectively create the edges between the new branch and the node. Additionally also the
	 * tags will be update to correspond with the new branch structure.
	 * 
	 * @param node
	 * @param oldBranch
	 * @param newBranch
	 * @return
	 */
	private SearchQueueBatch migrateNode(Node node, Branch oldBranch, Branch newBranch) {
		// Check whether the node already has an initial container and thus was already migrated
		if (node.getGraphFieldContainersIt(newBranch, INITIAL).iterator().hasNext()) {
			return null;
		}

		Node parent = node.getParentNode(oldBranch.getUuid());
		if (parent != null) {
			node.setParentNode(newBranch.getUuid(), parent);
		}

		node.getGraphFieldContainersIt(oldBranch, DRAFT).forEach(container -> {
			GraphFieldContainerEdgeImpl initialEdge = node.addFramedEdge(HAS_FIELD_CONTAINER, container, GraphFieldContainerEdgeImpl.class);
			initialEdge.setLanguageTag(container.getLanguage().getLanguageTag());
			initialEdge.setType(INITIAL);
			initialEdge.setBranchUuid(newBranch.getUuid());

			GraphFieldContainerEdgeImpl draftEdge = node.addFramedEdge(HAS_FIELD_CONTAINER, container, GraphFieldContainerEdgeImpl.class);
			draftEdge.setLanguageTag(container.getLanguage().getLanguageTag());
			draftEdge.setType(DRAFT);
			draftEdge.setBranchUuid(newBranch.getUuid());
			draftEdge.setSegmentInfo(parent, container.getSegmentFieldValue());
			draftEdge.setUrlFieldInfo(container.getUrlFieldValues());
		});
		SearchQueueBatch batch = searchQueue.create();
		batch.store(node, newBranch.getUuid(), DRAFT, false);

		node.getGraphFieldContainersIt(oldBranch, PUBLISHED).forEach(container -> {
			GraphFieldContainerEdgeImpl publishEdge = node.addFramedEdge(HAS_FIELD_CONTAINER, container, GraphFieldContainerEdgeImpl.class);
			publishEdge.setLanguageTag(container.getLanguage().getLanguageTag());
			publishEdge.setType(PUBLISHED);
			publishEdge.setBranchUuid(newBranch.getUuid());
			publishEdge.setSegmentInfo(parent, container.getSegmentFieldValue());
			publishEdge.setUrlFieldInfo(container.getUrlFieldValues());
		});
		batch.store(node, newBranch.getUuid(), PUBLISHED, false);

		// migrate tags
		node.getTags(oldBranch).forEach(tag -> node.addTag(tag, newBranch));
		return batch;

	}
}
