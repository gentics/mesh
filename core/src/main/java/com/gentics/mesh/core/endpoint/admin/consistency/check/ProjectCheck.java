package com.gentics.mesh.core.endpoint.admin.consistency.check;

import static com.gentics.mesh.core.data.relationship.GraphRelationships.HAS_BRANCH_ROOT;
import static com.gentics.mesh.core.data.relationship.GraphRelationships.HAS_MICROSCHEMA_ROOT;
import static com.gentics.mesh.core.data.relationship.GraphRelationships.HAS_NODE_ROOT;
import static com.gentics.mesh.core.data.relationship.GraphRelationships.HAS_PROJECT;
import static com.gentics.mesh.core.data.relationship.GraphRelationships.HAS_ROOT_NODE;
import static com.gentics.mesh.core.data.relationship.GraphRelationships.HAS_SCHEMA_ROOT;
import static com.gentics.mesh.core.data.relationship.GraphRelationships.HAS_TAGFAMILY_ROOT;
import static com.gentics.mesh.core.rest.admin.consistency.InconsistencySeverity.HIGH;
import static com.gentics.mesh.core.rest.admin.consistency.InconsistencySeverity.MEDIUM;
import static org.apache.commons.lang3.StringUtils.isEmpty;

import com.gentics.madl.tx.Tx;
import com.gentics.mesh.core.data.Project;
import com.gentics.mesh.core.data.impl.ProjectImpl;
import com.gentics.mesh.core.data.node.impl.NodeImpl;
import com.gentics.mesh.core.data.root.impl.BranchRootImpl;
import com.gentics.mesh.core.data.root.impl.NodeRootImpl;
import com.gentics.mesh.core.data.root.impl.ProjectMicroschemaContainerRootImpl;
import com.gentics.mesh.core.data.root.impl.ProjectRootImpl;
import com.gentics.mesh.core.data.root.impl.ProjectSchemaContainerRootImpl;
import com.gentics.mesh.core.data.root.impl.TagFamilyRootImpl;
import com.gentics.mesh.core.endpoint.admin.consistency.AbstractConsistencyCheck;
import com.gentics.mesh.core.endpoint.admin.consistency.ConsistencyCheckResult;
import com.gentics.mesh.graphdb.spi.Database;

/**
 * Project specific checks.
 */
public class ProjectCheck extends AbstractConsistencyCheck {

	@Override
	public String getName() {
		return "projects";
	}

	@Override
	public ConsistencyCheckResult invoke(Database db, Tx tx, boolean attemptRepair) {
		return processForType(db, ProjectImpl.class, (project, result) -> {
			checkProject(project, result);
		}, attemptRepair, tx);
	}

	private void checkProject(Project project, ConsistencyCheckResult result) {
		String uuid = project.getUuid();

		checkIn(project, HAS_PROJECT, ProjectRootImpl.class, result, HIGH);

		checkOut(project, HAS_BRANCH_ROOT, BranchRootImpl.class, result, HIGH);
		checkOut(project, HAS_NODE_ROOT, NodeRootImpl.class, result, HIGH);
		checkOut(project, HAS_TAGFAMILY_ROOT, TagFamilyRootImpl.class, result, HIGH);
		checkOut(project, HAS_ROOT_NODE, NodeImpl.class, result, HIGH);
		checkOut(project, HAS_SCHEMA_ROOT, ProjectSchemaContainerRootImpl.class, result, HIGH);
		checkOut(project, HAS_MICROSCHEMA_ROOT, ProjectMicroschemaContainerRootImpl.class, result, HIGH);

		// checkOut(project, HAS_CREATOR, UserImpl.class, response, MEDIUM);
		// checkOut(project, HAS_EDITOR, UserImpl.class, response, MEDIUM);

		if (isEmpty(project.getName())) {
			result.addInconsistency("Project name is empty or not set", uuid, HIGH);
		}
		if (project.getCreationTimestamp() == null) {
			result.addInconsistency("The project creation date is not set", uuid, MEDIUM);
		}
		if (project.getLastEditedTimestamp() == null) {
			result.addInconsistency("The project edit timestamp is not set", uuid, MEDIUM);
		}

//		checkBranchStructure(project, result);
	}

	// TODO Review previous/next branch concept
	// -
//	/**
//	 * Checks if the branches in the project are all connected with the HAS_NEXT_BRANCH edge.
//	 * Also checks if the project has an initial branch and a latest branch.
//	 * @param project
//	 * @param result
//	 */
//	private void checkBranchStructure(Project project, ConsistencyCheckResult result) {
//		long totalCount = project.getBranchRoot().findAll().count();
//		String uuid = project.getUuid();
//		if (totalCount == 0) {
//			result.addInconsistency("Project does not have any branches", uuid, HIGH);
//			return;
//		}
//		Branch branch = project.getInitialBranch();
//		if (branch == null) {
//			result.addInconsistency("Project does not have an initial branch", uuid, HIGH);
//			branch = project.getBranchRoot().getLatestBranch();
//			if (branch == null) {
//				result.addInconsistency("Project does not have a latest branch", uuid, HIGH);
//				branch = project.getBranchRoot().findAll().next();
//			}
//		}
//
//		Set<String> visited = new HashSet<>();
//		Stack<Branch> toVisit = new Stack<>();
//		toVisit.push(branch);
//		while (!toVisit.empty()) {
//			Branch current = toVisit.pop();
//			visited.add(current.getUuid());
//			Branch previousBranch = current.getPreviousBranch();
//			if (previousBranch != null && !visited.contains(previousBranch.getUuid())) {
//				toVisit.push(previousBranch);
//			}
//			current.getNextBranches().stream()
//				.filter(b -> !visited.contains(b.getUuid()))
//				.forEach(toVisit::push);
//		}
//		int amountVisited = visited.size();
//		if (amountVisited < totalCount) {
//			result.addInconsistency("Not all branches are connected together", uuid, MEDIUM);
//		} else if (amountVisited > totalCount) {
//			result.addInconsistency("Not all branches are connected to the BranchRoot", uuid, MEDIUM);
//		}
//	}

}
