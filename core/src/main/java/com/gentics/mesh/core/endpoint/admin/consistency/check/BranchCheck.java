package com.gentics.mesh.core.endpoint.admin.consistency.check;

import static com.gentics.mesh.core.data.relationship.GraphRelationships.ASSIGNED_TO_PROJECT;
import static com.gentics.mesh.core.data.relationship.GraphRelationships.HAS_BRANCH;
import static com.gentics.mesh.core.data.relationship.GraphRelationships.HAS_BRANCH_ROOT;
import static com.gentics.mesh.core.data.relationship.GraphRelationships.HAS_INITIAL_BRANCH;
import static com.gentics.mesh.core.data.relationship.GraphRelationships.HAS_LATEST_BRANCH;
import static com.gentics.mesh.core.rest.admin.consistency.InconsistencySeverity.HIGH;
import static com.gentics.mesh.core.rest.admin.consistency.InconsistencySeverity.MEDIUM;
import static org.apache.commons.lang3.StringUtils.isEmpty;

import com.gentics.mesh.core.data.Branch;
import com.gentics.mesh.core.data.impl.BranchImpl;
import com.gentics.mesh.core.data.impl.ProjectImpl;
import com.gentics.mesh.core.data.root.BranchRoot;
import com.gentics.mesh.core.data.root.impl.BranchRootImpl;
import com.gentics.mesh.core.endpoint.admin.consistency.AbstractConsistencyCheck;
import com.gentics.mesh.core.endpoint.admin.consistency.ConsistencyCheckResult;
import com.gentics.mesh.graphdb.spi.Database;
import com.syncleus.ferma.tx.Tx;

/**
 * Branch specific consistency checks.
 */
public class BranchCheck extends AbstractConsistencyCheck {

	@Override
	public String getName() {
		return "releases";
	}

	@Override
	public ConsistencyCheckResult invoke(Database db, Tx tx, boolean attemptRepair) {
		ConsistencyCheckResult a = processForType(db, BranchRootImpl.class, (root,result) -> {
			checkBranchRoot(root, result);
		}, attemptRepair, tx);

		ConsistencyCheckResult b = processForType(db, BranchImpl.class, (release, result)-> {
			checkBranch(release, result);
		}, attemptRepair, tx);

		return a.merge(b);
	}

	private void checkBranchRoot(BranchRoot releaseRoot, ConsistencyCheckResult result) {
		checkIn(releaseRoot, HAS_BRANCH_ROOT, ProjectImpl.class, result, HIGH);
		checkOut(releaseRoot, HAS_INITIAL_BRANCH, BranchImpl.class, result, HIGH);
		checkOut(releaseRoot, HAS_LATEST_BRANCH, BranchImpl.class, result, HIGH);
	}

	private void checkBranch(Branch release, ConsistencyCheckResult result) {
		String uuid = release.getUuid();

		checkIn(release, HAS_BRANCH, BranchRootImpl.class, result, HIGH);

		// checkOut(release, HAS_CREATOR, UserImpl.class, response, MEDIUM);
		// checkOut(release, HAS_EDITOR, UserImpl.class, response, MEDIUM);

		checkOut(release, ASSIGNED_TO_PROJECT, ProjectImpl.class, result, HIGH, in(HAS_BRANCH, BranchRootImpl.class), in(HAS_BRANCH_ROOT,
			ProjectImpl.class));

		if (isEmpty(release.getName())) {
			result.addInconsistency("Branch name is empty or not set", uuid, HIGH);
		}
		if (release.getCreationTimestamp() == null) {
			result.addInconsistency("The release creation date is not set", uuid, MEDIUM);
		}
		if (release.getLastEditedTimestamp() == null) {
			result.addInconsistency("The release edit timestamp is not set", uuid, MEDIUM);
		}
	}

}
