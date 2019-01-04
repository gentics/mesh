package com.gentics.mesh.core.endpoint.admin.consistency.check;

import static com.gentics.mesh.core.data.relationship.GraphRelationships.HAS_GROUP;
import static com.gentics.mesh.core.rest.admin.consistency.InconsistencySeverity.HIGH;
import static com.gentics.mesh.core.rest.admin.consistency.InconsistencySeverity.MEDIUM;
import static org.apache.commons.lang3.StringUtils.isEmpty;

import java.util.Iterator;

import com.gentics.mesh.core.data.Group;
import com.gentics.mesh.core.data.impl.GroupImpl;
import com.gentics.mesh.core.data.root.impl.GroupRootImpl;
import com.gentics.mesh.core.endpoint.admin.consistency.ConsistencyCheck;
import com.gentics.mesh.core.rest.admin.consistency.ConsistencyCheckResponse;
import com.gentics.mesh.graphdb.spi.LegacyDatabase;

/**
 * Group specific consistency checks.
 */
public class GroupCheck implements ConsistencyCheck {

	@Override
	public void invoke(LegacyDatabase db, ConsistencyCheckResponse response, boolean attemptRepair) {
		Iterator<? extends Group> it = db.getVerticesForType(GroupImpl.class);
		while (it.hasNext()) {
			checkGroup(it.next(), response);
		}
	}

	private void checkGroup(Group group, ConsistencyCheckResponse response) {
		String uuid = group.getUuid();

		checkIn(group, HAS_GROUP, GroupRootImpl.class, response, HIGH);

		// checkOut(group, HAS_CREATOR, UserImpl.class, response, MEDIUM);
		// checkOut(group, HAS_EDITOR, UserImpl.class, response, MEDIUM);

		if (isEmpty(group.getName())) {
			response.addInconsistency("Group has no valid name", uuid, MEDIUM);
		}
		if (group.getCreationTimestamp() == null) {
			response.addInconsistency("The group creation date is not set", uuid, MEDIUM);
		}
		if (group.getLastEditedTimestamp() == null) {
			response.addInconsistency("The group edit timestamp is not set", uuid, MEDIUM);
		}

	}

}
