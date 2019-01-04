package com.gentics.mesh.core.endpoint.admin.consistency.check;

import static com.gentics.mesh.core.rest.admin.consistency.InconsistencySeverity.HIGH;
import static com.gentics.mesh.core.rest.admin.consistency.InconsistencySeverity.MEDIUM;
import static org.apache.commons.lang3.StringUtils.isEmpty;

import java.util.Iterator;

import com.gentics.mesh.core.data.Role;
import com.gentics.mesh.core.data.impl.RoleImpl;
import com.gentics.mesh.core.endpoint.admin.consistency.ConsistencyCheck;
import com.gentics.mesh.core.rest.admin.consistency.ConsistencyCheckResponse;
import com.gentics.mesh.graphdb.spi.LegacyDatabase;

/**
 * Role specific consistency checks.
 */
public class RoleCheck implements ConsistencyCheck {

	@Override
	public void invoke(LegacyDatabase db, ConsistencyCheckResponse response, boolean attemptRepair) {
		Iterator<? extends Role> it = db.getVerticesForType(RoleImpl.class);
		while (it.hasNext()) {
			checkRole(it.next(), response);
		}
	}

	private void checkRole(Role role, ConsistencyCheckResponse response) {
		String uuid = role.getUuid();

		// checkOut(role, HAS_CREATOR, UserImpl.class, response, MEDIUM);
		// checkOut(role, HAS_EDITOR, UserImpl.class, response, MEDIUM);

		if (isEmpty(role.getName())) {
			response.addInconsistency("Role name is not set or empty", uuid, HIGH);
		}
		if (role.getCreationTimestamp() == null) {
			response.addInconsistency("The role creation date is not set", uuid, MEDIUM);
		}
		if (role.getLastEditedTimestamp() == null) {
			response.addInconsistency("The role edit timestamp is not set", uuid, MEDIUM);
		}
	}

}
