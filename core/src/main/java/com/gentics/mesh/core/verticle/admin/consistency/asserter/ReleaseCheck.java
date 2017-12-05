package com.gentics.mesh.core.verticle.admin.consistency.asserter;

import static com.gentics.mesh.core.data.relationship.GraphRelationships.ASSIGNED_TO_PROJECT;
import static com.gentics.mesh.core.data.relationship.GraphRelationships.HAS_CREATOR;
import static com.gentics.mesh.core.data.relationship.GraphRelationships.HAS_EDITOR;
import static com.gentics.mesh.core.data.relationship.GraphRelationships.HAS_INITIAL_RELEASE;
import static com.gentics.mesh.core.data.relationship.GraphRelationships.HAS_LATEST_RELEASE;
import static com.gentics.mesh.core.data.relationship.GraphRelationships.HAS_RELEASE;
import static com.gentics.mesh.core.data.relationship.GraphRelationships.HAS_RELEASE_ROOT;
import static com.gentics.mesh.core.rest.admin.consistency.InconsistencySeverity.HIGH;
import static com.gentics.mesh.core.rest.admin.consistency.InconsistencySeverity.MEDIUM;
import static org.apache.commons.lang3.StringUtils.isEmpty;

import java.util.Iterator;

import com.gentics.mesh.core.data.Release;
import com.gentics.mesh.core.data.impl.ProjectImpl;
import com.gentics.mesh.core.data.impl.ReleaseImpl;
import com.gentics.mesh.core.data.impl.UserImpl;
import com.gentics.mesh.core.data.root.ReleaseRoot;
import com.gentics.mesh.core.data.root.impl.ReleaseRootImpl;
import com.gentics.mesh.core.rest.admin.consistency.ConsistencyCheckResponse;
import com.gentics.mesh.core.verticle.admin.consistency.ConsistencyCheck;
import com.gentics.mesh.graphdb.spi.Database;

/**
 * Release specific consistency checks.
 */
public class ReleaseCheck implements ConsistencyCheck {

	@Override
	public void invoke(Database db, ConsistencyCheckResponse response) {
		Iterator<? extends ReleaseRoot> it = db.getVerticesForType(ReleaseRootImpl.class);
		while (it.hasNext()) {
			checkReleaseRoot(it.next(), response);
		}

		Iterator<? extends Release> rIt = db.getVerticesForType(ReleaseImpl.class);
		while (rIt.hasNext()) {
			checkRelease(rIt.next(), response);
		}
	}

	private void checkReleaseRoot(ReleaseRoot releaseRoot, ConsistencyCheckResponse response) {
		checkIn(releaseRoot, HAS_RELEASE_ROOT, ProjectImpl.class, response, HIGH);
		checkOut(releaseRoot, HAS_INITIAL_RELEASE, ReleaseImpl.class, response, HIGH);
		checkOut(releaseRoot, HAS_LATEST_RELEASE, ReleaseImpl.class, response, HIGH);
	}

	private void checkRelease(Release release, ConsistencyCheckResponse response) {
		String uuid = release.getUuid();

		checkIn(release, HAS_RELEASE, ReleaseRootImpl.class, response, HIGH);

		checkOut(release, HAS_CREATOR, UserImpl.class, response, MEDIUM);
		checkOut(release, HAS_EDITOR, UserImpl.class, response, MEDIUM);
		checkOut(release, ASSIGNED_TO_PROJECT, ProjectImpl.class, response, HIGH,
				in(HAS_RELEASE, ReleaseRootImpl.class),
				in(HAS_RELEASE_ROOT, ProjectImpl.class));

		if (isEmpty(release.getName())) {
			response.addInconsistency("Release name is empty or not set", uuid, HIGH);
		}
		if (release.getCreationTimestamp() == null) {
			response.addInconsistency("The release creation date is not set", uuid, MEDIUM);
		}
		if (release.getLastEditedTimestamp() == null) {
			response.addInconsistency("The release edit timestamp is not set", uuid, MEDIUM);
		}
	}

}
