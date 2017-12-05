package com.gentics.mesh.core.verticle.admin.consistency.asserter;

import static com.gentics.mesh.core.rest.admin.consistency.InconsistencySeverity.MEDIUM;
import static org.apache.commons.lang3.StringUtils.isEmpty;

import java.util.Iterator;

import com.gentics.mesh.core.data.Group;
import com.gentics.mesh.core.data.impl.GroupImpl;
import com.gentics.mesh.core.rest.admin.consistency.ConsistencyCheckResponse;
import com.gentics.mesh.core.verticle.admin.consistency.ConsistencyCheck;
import com.gentics.mesh.graphdb.spi.Database;

/**
 * Group specific consistency checks.
 */
public class GroupCheck implements ConsistencyCheck {

	@Override
	public void invoke(Database db, ConsistencyCheckResponse response) {
		Iterator<? extends Group> it = db.getVerticesForType(GroupImpl.class);
		while (it.hasNext()) {
			checkGroup(it.next(), response);
		}
	}

	private void checkGroup(Group group, ConsistencyCheckResponse response) {
		String uuid = group.getUuid();

		if (isEmpty(group.getName())) {
			response.addInconsistency("Group has no valid name", uuid, MEDIUM);
		}
		if (group.getCreationTimestamp() == null) {
			response.addInconsistency("The group creation date is not set", uuid, MEDIUM);
		}
		if (group.getCreator() == null) {
			response.addInconsistency("The group creator is not set", uuid, MEDIUM);
		}
		if (group.getEditor() == null) {
			response.addInconsistency("The group editor is not set", uuid, MEDIUM);
		}
		if (group.getLastEditedTimestamp() == null) {
			response.addInconsistency("The group edit timestamp is not set", uuid, MEDIUM);
		}

	}

}
