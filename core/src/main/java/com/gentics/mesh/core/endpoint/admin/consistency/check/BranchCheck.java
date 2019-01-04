package com.gentics.mesh.core.endpoint.admin.consistency.check;

import static com.gentics.mesh.core.data.relationship.GraphRelationships.ASSIGNED_TO_PROJECT;
import static com.gentics.mesh.core.data.relationship.GraphRelationships.HAS_INITIAL_BRANCH;
import static com.gentics.mesh.core.data.relationship.GraphRelationships.HAS_LATEST_BRANCH;
import static com.gentics.mesh.core.data.relationship.GraphRelationships.HAS_BRANCH;
import static com.gentics.mesh.core.data.relationship.GraphRelationships.HAS_BRANCH_ROOT;
import static com.gentics.mesh.core.rest.admin.consistency.InconsistencySeverity.HIGH;
import static com.gentics.mesh.core.rest.admin.consistency.InconsistencySeverity.MEDIUM;
import static org.apache.commons.lang3.StringUtils.isEmpty;

import java.util.Iterator;

import com.gentics.mesh.core.data.Branch;
import com.gentics.mesh.core.data.impl.ProjectImpl;
import com.gentics.mesh.core.data.impl.BranchImpl;
import com.gentics.mesh.core.data.root.BranchRoot;
import com.gentics.mesh.core.data.root.impl.BranchRootImpl;
import com.gentics.mesh.core.endpoint.admin.consistency.ConsistencyCheck;
import com.gentics.mesh.core.rest.admin.consistency.ConsistencyCheckResponse;
import com.gentics.mesh.graphdb.spi.LegacyDatabase;

/**
 * Branch specific consistency checks.
 */
public class BranchCheck implements ConsistencyCheck {

	@Override
	public void invoke(LegacyDatabase db, ConsistencyCheckResponse response, boolean attemptRepair) {
		Iterator<? extends BranchRoot> it = db.getVerticesForType(BranchRootImpl.class);
		while (it.hasNext()) {
			checkBranchRoot(it.next(), response);
		}

		Iterator<? extends Branch> rIt = db.getVerticesForType(BranchImpl.class);
		while (rIt.hasNext()) {
			checkBranch(rIt.next(), response);
		}
	}

	private void checkBranchRoot(BranchRoot branchRoot, ConsistencyCheckResponse response) {
		checkIn(branchRoot, HAS_BRANCH_ROOT, ProjectImpl.class, response, HIGH);
		checkOut(branchRoot, HAS_INITIAL_BRANCH, BranchImpl.class, response, HIGH);
		checkOut(branchRoot, HAS_LATEST_BRANCH, BranchImpl.class, response, HIGH);
	}

	private void checkBranch(Branch branch, ConsistencyCheckResponse response) {
		String uuid = branch.getUuid();

		checkIn(branch, HAS_BRANCH, BranchRootImpl.class, response, HIGH);

		// if the branch is not the initial branch, it must have a previous branch
		if (branch.in(HAS_INITIAL_BRANCH).nextOrDefaultExplicit(BranchRootImpl.class, null) == null && branch.getPreviousBranch() == null) {
			response.addInconsistency("Branch must either be the initial branch or have a previous branch", uuid, HIGH);
		}

		// checkOut(branch, HAS_CREATOR, UserImpl.class, response, MEDIUM);
		// checkOut(branch, HAS_EDITOR, UserImpl.class, response, MEDIUM);

		checkOut(branch, ASSIGNED_TO_PROJECT, ProjectImpl.class, response, HIGH, in(HAS_BRANCH, BranchRootImpl.class), in(HAS_BRANCH_ROOT,
				ProjectImpl.class));

		if (isEmpty(branch.getName())) {
			response.addInconsistency("Branch name is empty or not set", uuid, HIGH);
		}
		if (branch.getCreationTimestamp() == null) {
			response.addInconsistency("The branch creation date is not set", uuid, MEDIUM);
		}
		if (branch.getLastEditedTimestamp() == null) {
			response.addInconsistency("The branch edit timestamp is not set", uuid, MEDIUM);
		}
	}

}
