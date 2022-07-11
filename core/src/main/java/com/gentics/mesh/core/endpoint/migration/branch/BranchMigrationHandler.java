package com.gentics.mesh.core.endpoint.migration.branch;

import static com.gentics.mesh.core.data.relationship.GraphRelationships.HAS_FIELD_CONTAINER;
import static com.gentics.mesh.core.rest.common.ContainerType.DRAFT;
import static com.gentics.mesh.core.rest.common.ContainerType.INITIAL;
import static com.gentics.mesh.core.rest.common.ContainerType.PUBLISHED;
import static com.gentics.mesh.core.rest.job.JobStatus.RUNNING;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.List;
import java.util.Queue;
import java.util.stream.Collectors;

import javax.inject.Inject;
import javax.inject.Provider;
import javax.inject.Singleton;

import com.gentics.mesh.context.BranchMigrationContext;
import com.gentics.mesh.core.data.Branch;
import com.gentics.mesh.core.data.NodeGraphFieldContainer;
import com.gentics.mesh.core.data.Project;
import com.gentics.mesh.core.data.impl.GraphFieldContainerEdgeImpl;
import com.gentics.mesh.core.data.node.Node;
import com.gentics.mesh.core.endpoint.migration.AbstractMigrationHandler;
import com.gentics.mesh.core.endpoint.migration.MigrationStatusHandler;
import com.gentics.mesh.core.endpoint.node.BinaryUploadHandler;
import com.gentics.mesh.core.rest.event.node.BranchMigrationCause;
import com.gentics.mesh.event.EventQueueBatch;
import com.gentics.mesh.graphdb.spi.Database;
import com.gentics.mesh.madl.traversal.TraversalResult;
import com.gentics.mesh.metric.MetricsService;

import io.reactivex.Completable;
import io.reactivex.exceptions.CompositeException;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;

@Singleton
public class BranchMigrationHandler extends AbstractMigrationHandler {

	private static final Logger log = LoggerFactory.getLogger(BranchMigrationHandler.class);

	@Inject
	public BranchMigrationHandler(Database db, BinaryUploadHandler nodeFieldAPIHandler, MetricsService metrics, Provider<EventQueueBatch> batchProvider) {
		super(db, nodeFieldAPIHandler, metrics, batchProvider);
	}

	/**
	 * Migrate all nodes from one branch to the other
	 * 
	 * @param context
	 * @return
	 */
	public Completable migrateBranch(BranchMigrationContext context) {
		context.validate();
		return Completable.defer(() -> {
			Branch oldBranch = context.getOldBranch();
			Branch newBranch = context.getNewBranch();
			BranchMigrationCause cause = context.getCause();
			MigrationStatusHandler status = context.getStatus();

			db.tx(() -> {
				if (status != null) {
					status.setStatus(RUNNING);
					status.commit();
				}
			});

			Queue<? extends Node> nodes = db.tx(() -> {
				Project project = oldBranch.getProject();
				return new ArrayDeque<>(project.findNodes().list());
			});

			List<Exception> errorsDetected = new ArrayList<>();
			// Iterate over all nodes of the project and migrate them to the new branch
			migrateLoop(nodes, cause, status, (batch, node, errors) -> {
				migrateNode(node, batch, oldBranch, newBranch, errorsDetected);
			});

			if (!errorsDetected.isEmpty()) {
				log.info("Encountered {" + errorsDetected.size() + "} errors during micronode migration.");
			}

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
		});

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
				if (node.getGraphFieldContainers(newBranch, INITIAL).hasNext()) {
					return;
				}

				Node parent = node.getParentNode(oldBranch.getUuid());
				if (parent != null) {
					node.setParentNode(newBranch.getUuid(), parent);
				}

				TraversalResult<? extends NodeGraphFieldContainer> drafts = node.getGraphFieldContainers(oldBranch, DRAFT);
				TraversalResult<? extends NodeGraphFieldContainer> published = node.getGraphFieldContainers(oldBranch, PUBLISHED);

				// 1. Migrate draft containers first
				drafts.forEach(container -> {
					// We only need to set the initial edge if there are no published containers.
					// Otherwise the initial edge will be set using the published container.
					if (!published.hasNext()) {
						setInitial(node, container, newBranch);
					}

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
					draftEdge.setUrlFieldInfo(container.getUrlFieldValues().collect(Collectors.toSet()));
					batch.add(container.onUpdated(newBranch.getUuid(), DRAFT));
				});

				// 2. Migrate published containers
				published.forEach(container -> {
					// Set the initial edge for published containers since the published container may be an older version and created before the draft container was created.
					// The initial edge should always point to the oldest container of either draft or published.
					setInitial(node, container, newBranch);

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
					publishEdge.setUrlFieldInfo(container.getUrlFieldValues().collect(Collectors.toSet()));
					batch.add(container.onUpdated(newBranch.getUuid(), PUBLISHED));
				});

				// Migrate tags
				node.getTags(oldBranch).forEach(tag -> node.addTag(tag, newBranch));
			});
		} catch (Exception e1) {
			log.error("Error while handling node {" + node.getUuid() + "} during schema migration.", e1);
			errorsDetected.add(e1);
		}
	}

	/**
	 * Create a new initial edge between node and container for the given branch.
	 */
	private void setInitial(Node node, NodeGraphFieldContainer container, Branch branch) {
		GraphFieldContainerEdgeImpl initialEdge = node.addFramedEdge(HAS_FIELD_CONTAINER, container,
			GraphFieldContainerEdgeImpl.class);
		initialEdge.setLanguageTag(container.getLanguageTag());
		initialEdge.setBranchUuid(branch.getUuid());
		initialEdge.setType(INITIAL);
	}
}
