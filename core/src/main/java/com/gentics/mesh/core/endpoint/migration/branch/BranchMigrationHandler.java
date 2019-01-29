package com.gentics.mesh.core.endpoint.migration.branch;

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

import com.gentics.mesh.core.data.Branch;
import com.gentics.mesh.core.data.Project;
import com.gentics.mesh.core.data.impl.GraphFieldContainerEdgeImpl;
import com.gentics.mesh.core.data.node.Node;
import com.gentics.mesh.core.endpoint.migration.AbstractMigrationHandler;
import com.gentics.mesh.core.endpoint.migration.MigrationStatusHandler;
import com.gentics.mesh.core.endpoint.node.BinaryFieldHandler;
import com.gentics.mesh.event.EventQueueBatch;
import com.gentics.mesh.graphdb.spi.Database;

import io.reactivex.Completable;
import io.reactivex.exceptions.CompositeException;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;

@Singleton
public class BranchMigrationHandler extends AbstractMigrationHandler {

	private static final Logger log = LoggerFactory.getLogger(BranchMigrationHandler.class);

	@Inject
	public BranchMigrationHandler(Database db, BinaryFieldHandler nodeFieldAPIHandler) {
		super(db, nodeFieldAPIHandler);
	}

	/**
	 * Migrate all nodes from one branch to the other
	 * 
	 * @param newBranch
	 *            new branch
	 * @param status
	 * @return
	 */
	public Completable migrateBranch(Branch newBranch, MigrationStatusHandler status) {
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
		List<Exception> errorsDetected = new ArrayList<>();
		EventQueueBatch sqb = null;
		for (Node node : project.getNodeRoot().findAll()) {
			// Create a new SQB to handle the ES update
			if (sqb == null) {
				sqb = EventQueueBatch.create();
			}
			migrateNode(node, sqb, oldBranch, newBranch, errorsDetected);
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
				sqb.dispatch();
				sqb = null;
			}
		}
		if (sqb != null) {
			log.info("Syncing last batch with size: " + sqb.size());
			sqb.dispatch();
			sqb = null;
		}

		log.info("Migration of " + count + " node done..");
		log.info("Encountered {" + errorsDetected.size() + "} errors during micronode migration.");

		Completable result = Completable.complete();
		if (!errorsDetected.isEmpty()) {
			if (log.isDebugEnabled()) {
				for (Exception error : errorsDetected) {
					log.error("Encountered migration error.", error);
				}
			}
			result = Completable.error(new CompositeException(errorsDetected));
		} else {
			db.tx(() -> {
				newBranch.setMigrated(true);
			});
		}
		return result;

	}

	/**
	 * Migrate the node from the old branch to the new branch. This will effectively create the edges between the new branch and the node. Additionally also the
	 * tags will be update to correspond with the new branch structure.
	 * 
	 * @param node
	 * @param batch
	 * @param oldBranch
	 * @param newBranc
	 * @param errorsDetected
	 */
	private void migrateNode(Node node, EventQueueBatch batch, Branch oldBranch, Branch newBranch, List<Exception> errorsDetected) {
		try {
			db.tx((tx) -> {

				// Check whether the node already has an initial container and thus was already migrated
				if (node.getGraphFieldContainersIt(newBranch, INITIAL).iterator().hasNext()) {
					return;
				}

				Node parent = node.getParentNode(oldBranch.getUuid());
				if (parent != null) {
					node.setParentNode(newBranch.getUuid(), parent);
				}

				node.getGraphFieldContainersIt(oldBranch, DRAFT).forEach(container -> {
					GraphFieldContainerEdgeImpl initialEdge = node.addFramedEdge(HAS_FIELD_CONTAINER, container, GraphFieldContainerEdgeImpl.class);
					initialEdge.setLanguageTag(container.getLanguageTag());
					initialEdge.setType(INITIAL);
					initialEdge.setBranchUuid(newBranch.getUuid());

					GraphFieldContainerEdgeImpl draftEdge = node.addFramedEdge(HAS_FIELD_CONTAINER, container, GraphFieldContainerEdgeImpl.class);
					draftEdge.setLanguageTag(container.getLanguageTag());
					draftEdge.setType(DRAFT);
					draftEdge.setBranchUuid(newBranch.getUuid());
					String value = container.getSegmentFieldValue();
					if (value != null) {
						draftEdge.setSegmentInfo(parent, value);
					} else {
						draftEdge.setSegmentInfo(null);
					}
					draftEdge.setUrlFieldInfo(container.getUrlFieldValues());
				});
				batch.store(node, newBranch.getUuid(), DRAFT, false);

				node.getGraphFieldContainersIt(oldBranch, PUBLISHED).forEach(container -> {
					GraphFieldContainerEdgeImpl publishEdge = node.addFramedEdge(HAS_FIELD_CONTAINER, container, GraphFieldContainerEdgeImpl.class);
					publishEdge.setLanguageTag(container.getLanguageTag());
					publishEdge.setType(PUBLISHED);
					publishEdge.setBranchUuid(newBranch.getUuid());
					String value = container.getSegmentFieldValue();
					if (value != null) {
						publishEdge.setSegmentInfo(parent, value);
					} else {
						publishEdge.setSegmentInfo(null);
					}
					publishEdge.setUrlFieldInfo(container.getUrlFieldValues());
				});
				batch.store(node, newBranch.getUuid(), PUBLISHED, false);

				// migrate tags
				node.getTags(oldBranch).forEach(tag -> node.addTag(tag, newBranch));
			});
		} catch (Exception e1) {
			log.error("Error while handling node {" + node.getUuid() + "} during schema migration.", e1);
			errorsDetected.add(e1);
		}
	}
}
