package com.gentics.mesh.core.migration.impl;

import static com.gentics.mesh.core.rest.common.ContainerType.DRAFT;
import static com.gentics.mesh.core.rest.common.ContainerType.INITIAL;
import static com.gentics.mesh.core.rest.common.ContainerType.PUBLISHED;
import static com.gentics.mesh.core.rest.job.JobStatus.RUNNING;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.List;
import java.util.Queue;

import javax.inject.Inject;
import javax.inject.Provider;
import javax.inject.Singleton;

import com.gentics.mesh.context.BranchMigrationContext;
import com.gentics.mesh.core.data.HibNodeFieldContainer;
import com.gentics.mesh.core.data.branch.HibBranch;
import com.gentics.mesh.core.data.dao.NodeDao;
import com.gentics.mesh.core.data.dao.PersistingContentDao;
import com.gentics.mesh.core.data.dao.TagDao;
import com.gentics.mesh.core.data.node.HibNode;
import com.gentics.mesh.core.data.project.HibProject;
import com.gentics.mesh.core.db.CommonTx;
import com.gentics.mesh.core.db.Database;
import com.gentics.mesh.core.endpoint.migration.MigrationStatusHandler;
import com.gentics.mesh.core.endpoint.node.BinaryUploadHandlerImpl;
import com.gentics.mesh.core.migration.AbstractMigrationHandler;
import com.gentics.mesh.core.migration.BranchMigration;
import com.gentics.mesh.core.rest.event.node.BranchMigrationCause;
import com.gentics.mesh.core.result.Result;
import com.gentics.mesh.event.EventQueueBatch;
import com.gentics.mesh.metric.MetricsService;

import io.reactivex.Completable;
import io.reactivex.exceptions.CompositeException;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;

/**
 * @see BranchMigration
 */
@Singleton
public class BranchMigrationImpl extends AbstractMigrationHandler implements BranchMigration {

	private static final Logger log = LoggerFactory.getLogger(BranchMigrationImpl.class);

	@Inject
	public BranchMigrationImpl(Database db, BinaryUploadHandlerImpl nodeFieldAPIHandler, MetricsService metrics, Provider<EventQueueBatch> batchProvider) {
		super(db, nodeFieldAPIHandler, metrics, batchProvider);
	}

	@Override
	public Completable migrateBranch(BranchMigrationContext context) {
		context.validate();
		return Completable.defer(() -> {
			HibBranch oldBranch = context.getOldBranch();
			HibBranch newBranch = context.getNewBranch();
			BranchMigrationCause cause = context.getCause();
			MigrationStatusHandler status = context.getStatus();

			db.tx(() -> {
				if (status != null) {
					status.setStatus(RUNNING);
					status.commit();
				}
			});

			Queue<? extends HibNode> nodes = db.tx(tx -> {
				HibProject project = oldBranch.getProject();
				return new ArrayDeque<>(tx.nodeDao().findAll(project).list());
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
	 * @param newBranch
	 * @param errorsDetected
	 */
	private void migrateNode(HibNode node, EventQueueBatch batch, HibBranch oldBranch, HibBranch newBranch, List<Exception> errorsDetected) {
		try {
			db.tx((tx) -> {
				NodeDao nodeDao = tx.nodeDao();
				TagDao tagDao = tx.tagDao();
				PersistingContentDao contentDao = tx.<CommonTx>unwrap().contentDao();

				// Check whether the node already has an initial container and thus was already migrated
				if (contentDao.getFieldContainers(node, newBranch, INITIAL).hasNext()) {
					return;
				}

				HibNode parent = nodeDao.getParentNode(node, oldBranch.getUuid());
				if (parent != null) {
					nodeDao.setParentNode(node, newBranch.getUuid(), parent);
				}

				Result<HibNodeFieldContainer> drafts = contentDao.getFieldContainers(node, oldBranch, DRAFT);
				Result<HibNodeFieldContainer> published = contentDao.getFieldContainers(node, oldBranch, PUBLISHED);

				// 1. Migrate draft containers first
				drafts.forEach(container -> {
					// We only need to set the initial edge if there are no published containers.
					// Otherwise the initial edge will be set using the published container.
					contentDao.migrateContainerOntoBranch(container, newBranch, node, batch, DRAFT, !published.hasNext());
				});

				// 2. Migrate published containers
				published.forEach(container -> {
					// Set the initial edge for published containers since the published container may be an older version and created before the draft container was created.
					// The initial edge should always point to the oldest container of either draft or published.
					contentDao.migrateContainerOntoBranch(container, newBranch, node, batch, PUBLISHED, true);
				});

				// Migrate tags
				tagDao.getTags(node, oldBranch).forEach(tag -> tagDao.addTag(node, tag, newBranch));
			});
		} catch (Exception e1) {
			log.error("Error while handling node {" + node.getUuid() + "} during schema migration.", e1);
			errorsDetected.add(e1);
		}
	}
}
