package com.gentics.mesh.core.endpoint.admin.consistency.check;

import static com.gentics.mesh.core.data.relationship.GraphRelationships.ASSIGNED_TO_PROJECT;
import static com.gentics.mesh.core.data.relationship.GraphRelationships.HAS_INITIAL_RELEASE;
import static com.gentics.mesh.core.data.relationship.GraphRelationships.HAS_LATEST_RELEASE;
import static com.gentics.mesh.core.data.relationship.GraphRelationships.HAS_RELEASE;
import static com.gentics.mesh.core.data.relationship.GraphRelationships.HAS_RELEASE_ROOT;
import static com.gentics.mesh.core.rest.admin.consistency.InconsistencySeverity.HIGH;
import static com.gentics.mesh.core.rest.admin.consistency.InconsistencySeverity.MEDIUM;
import static org.apache.commons.lang3.StringUtils.isEmpty;

import com.gentics.mesh.core.data.Release;
import com.gentics.mesh.core.data.impl.ProjectImpl;
import com.gentics.mesh.core.data.impl.ReleaseImpl;
import com.gentics.mesh.core.data.root.ReleaseRoot;
import com.gentics.mesh.core.data.root.impl.ReleaseRootImpl;
import com.gentics.mesh.core.endpoint.admin.consistency.AbstractConsistencyCheck;
import com.gentics.mesh.core.endpoint.admin.consistency.ConsistencyCheckResult;
import com.gentics.mesh.graphdb.spi.Database;
import com.syncleus.ferma.tx.Tx;

/**
 * Release specific consistency checks.
 */
public class ReleaseCheck extends AbstractConsistencyCheck {

	@Override
	public String getName() {
		return "releases";
	}

	@Override
	public ConsistencyCheckResult invoke(Database db, Tx tx, boolean attemptRepair) {
		ConsistencyCheckResult a = processForType(db, ReleaseRootImpl.class, (root,result) -> {
			checkReleaseRoot(root, result);
		}, attemptRepair, tx);

		ConsistencyCheckResult b = processForType(db, ReleaseImpl.class, (release, result)-> {
			checkRelease(release, result);
		}, attemptRepair, tx);

		return a.merge(b);
	}

	private void checkReleaseRoot(ReleaseRoot releaseRoot, ConsistencyCheckResult result) {
		checkIn(releaseRoot, HAS_RELEASE_ROOT, ProjectImpl.class, result, HIGH);
		checkOut(releaseRoot, HAS_INITIAL_RELEASE, ReleaseImpl.class, result, HIGH);
		checkOut(releaseRoot, HAS_LATEST_RELEASE, ReleaseImpl.class, result, HIGH);
	}

	private void checkRelease(Release release, ConsistencyCheckResult result) {
		String uuid = release.getUuid();

		checkIn(release, HAS_RELEASE, ReleaseRootImpl.class, result, HIGH);

		// checkOut(release, HAS_CREATOR, UserImpl.class, response, MEDIUM);
		// checkOut(release, HAS_EDITOR, UserImpl.class, response, MEDIUM);

		checkOut(release, ASSIGNED_TO_PROJECT, ProjectImpl.class, result, HIGH, in(HAS_RELEASE, ReleaseRootImpl.class), in(HAS_RELEASE_ROOT,
			ProjectImpl.class));

		if (isEmpty(release.getName())) {
			result.addInconsistency("Release name is empty or not set", uuid, HIGH);
		}
		if (release.getCreationTimestamp() == null) {
			result.addInconsistency("The release creation date is not set", uuid, MEDIUM);
		}
		if (release.getLastEditedTimestamp() == null) {
			result.addInconsistency("The release edit timestamp is not set", uuid, MEDIUM);
		}
	}

}
