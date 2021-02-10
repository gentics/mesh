package com.gentics.mesh.core.endpoint.admin.consistency.check;

import static com.gentics.mesh.core.data.relationship.GraphRelationships.HAS_GROUP;
import static com.gentics.mesh.core.rest.admin.consistency.InconsistencySeverity.HIGH;
import static com.gentics.mesh.core.rest.admin.consistency.InconsistencySeverity.MEDIUM;
import static org.apache.commons.lang3.StringUtils.isEmpty;

import com.gentics.mesh.core.data.Group;
import com.gentics.mesh.core.data.Tx;
import com.gentics.mesh.core.data.impl.GroupImpl;
import com.gentics.mesh.core.data.root.impl.GroupRootImpl;
import com.gentics.mesh.core.endpoint.admin.consistency.AbstractConsistencyCheck;
import com.gentics.mesh.core.endpoint.admin.consistency.ConsistencyCheckResult;
import com.gentics.mesh.graphdb.spi.Database;

/**
 * Group specific consistency checks.
 */
public class GroupCheck extends AbstractConsistencyCheck {

	@Override
	public String getName() {
		return "groups";
	}

	@Override
	public ConsistencyCheckResult invoke(Database db, Tx tx, boolean attemptRepair) {
		return processForType(db, GroupImpl.class, (group, result) -> {
			checkGroup(group, result);
		}, attemptRepair, tx);
	}

	private void checkGroup(Group group, ConsistencyCheckResult result) {
		String uuid = group.getUuid();

		checkIn(group, HAS_GROUP, GroupRootImpl.class, result, HIGH);

		// checkOut(group, HAS_CREATOR, UserImpl.class, response, MEDIUM);
		// checkOut(group, HAS_EDITOR, UserImpl.class, response, MEDIUM);

		if (isEmpty(group.getName())) {
			result.addInconsistency("Group has no valid name", uuid, MEDIUM);
		}
		if (group.getCreationTimestamp() == null) {
			result.addInconsistency("The group creation date is not set", uuid, MEDIUM);
		}
		if (group.getLastEditedTimestamp() == null) {
			result.addInconsistency("The group edit timestamp is not set", uuid, MEDIUM);
		}
		if (group.getBucketId() == null) {
			result.addInconsistency("The group bucket id is not set", uuid, MEDIUM);
		}

	}

}
