package com.gentics.mesh.check;

import static com.gentics.mesh.core.rest.common.ContainerType.DRAFT;
import static com.gentics.mesh.core.rest.common.ContainerType.PUBLISHED;

import java.util.List;
import java.util.stream.Collectors;

import com.gentics.mesh.context.impl.DummyEventQueueBatch;
import com.gentics.mesh.core.data.Language;
import com.gentics.mesh.core.data.NodeFieldContainer;
import com.gentics.mesh.core.data.branch.Branch;
import com.gentics.mesh.core.data.dao.BranchDao;
import com.gentics.mesh.core.data.dao.ContentDao;
import com.gentics.mesh.core.data.dao.LanguageDao;
import com.gentics.mesh.core.data.dao.PersistingContentDao;
import com.gentics.mesh.core.data.dao.ProjectDao;
import com.gentics.mesh.core.data.dao.TagDao;
import com.gentics.mesh.core.data.node.Node;
import com.gentics.mesh.core.data.project.Project;
import com.gentics.mesh.core.db.CommonTx;
import com.gentics.mesh.core.db.Database;
import com.gentics.mesh.core.db.Tx;
import com.gentics.mesh.core.endpoint.admin.consistency.ConsistencyCheckResult;
import com.gentics.mesh.core.rest.admin.consistency.InconsistencyInfo;
import com.gentics.mesh.core.rest.admin.consistency.InconsistencySeverity;
import com.gentics.mesh.core.rest.admin.consistency.RepairAction;
import com.gentics.mesh.core.result.Result;
import com.gentics.mesh.event.EventQueueBatch;

public class HibernateBranchCheck extends AbstractHibernateConsistencyCheck {

	@Override
	public boolean asyncOnly() {
		return false;
	}

	@Override
	public ConsistencyCheckResult invoke(Database db, Tx tx, boolean attemptRepair) {
		ConsistencyCheckResult result = new ConsistencyCheckResult();
		ProjectDao projectDao = tx.projectDao();
		BranchDao branchDao = tx.branchDao();
		ContentDao contentDao = tx.contentDao();
		LanguageDao languageDao = tx.languageDao();
		List<String> languageTags = languageDao.findAll().stream().map(Language::getLanguageTag).collect(Collectors.toList());

		for (Project project : projectDao.findAll()) {
			Node baseNode = project.getBaseNode();
			Branch initialBranch = project.getInitialBranch();
			for (Branch branch : branchDao.findAll(project)) {
				NodeFieldContainer content = contentDao.findVersion(baseNode, languageTags, branch.getUuid(), "draft");
				if (content == null) {
					InconsistencyInfo info = new InconsistencyInfo()
						.setDescription("Branch does not contain the project root node")
						.setElementUuid(branch.getUuid())
						.setSeverity(InconsistencySeverity.HIGH)
						.setRepairAction(RepairAction.RECOVER);
					if (attemptRepair) {
						migrateRootNode(tx, baseNode, initialBranch, branch);
						info.setRepaired(true);
					}
					result.addInconsistency(info);
				}
			}
		}

		return result;
	}

	@Override
	public String getName() {
		return "branches";
	}

	/**
	 * Migrate the root node from the old branch into the given branch
	 * @param tx transaction
	 * @param node node to migrate
	 * @param oldBranch old branch
	 * @param branchToMigrateTo target branch
	 */
	protected void migrateRootNode(Tx tx, Node node, Branch oldBranch, Branch branchToMigrateTo) {
		PersistingContentDao contentDao = tx.<CommonTx>unwrap().contentDao();
		TagDao tagDao = tx.tagDao();

		EventQueueBatch batch = new DummyEventQueueBatch();

		Result<NodeFieldContainer> drafts = contentDao.getFieldContainers(node, oldBranch, DRAFT);
		Result<NodeFieldContainer> published = contentDao.getFieldContainers(node, oldBranch, PUBLISHED);

		// 1. Migrate draft containers first
		drafts.forEach(container -> {
			// We only need to set the initial edge if there are no published containers.
			// Otherwise the initial edge will be set using the published container.
			contentDao.migrateContainerOntoBranch(container, branchToMigrateTo, node, batch, DRAFT, !published.hasNext());
		});

		// 2. Migrate published containers
		published.forEach(container -> {
			// Set the initial edge for published containers since the published container may be an older version and created before the draft container was created.
			// The initial edge should always point to the oldest container of either draft or published.
			contentDao.migrateContainerOntoBranch(container, branchToMigrateTo, node, batch, PUBLISHED, true);
		});

		// Migrate tags
		tagDao.getTags(node, oldBranch).forEach(tag -> tagDao.addTag(node, tag, branchToMigrateTo));
	}
}
