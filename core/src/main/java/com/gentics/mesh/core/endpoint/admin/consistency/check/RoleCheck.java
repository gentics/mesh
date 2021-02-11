package com.gentics.mesh.core.endpoint.admin.consistency.check;

import static com.gentics.mesh.core.rest.admin.consistency.InconsistencySeverity.HIGH;
import static com.gentics.mesh.core.rest.admin.consistency.InconsistencySeverity.MEDIUM;
import static org.apache.commons.lang3.StringUtils.isEmpty;

import com.gentics.mesh.core.data.Role;
import com.gentics.mesh.core.data.impl.RoleImpl;
import com.gentics.mesh.core.db.Tx;
import com.gentics.mesh.core.endpoint.admin.consistency.AbstractConsistencyCheck;
import com.gentics.mesh.core.endpoint.admin.consistency.ConsistencyCheckResult;
import com.gentics.mesh.graphdb.spi.Database;

/**
 * Role specific consistency checks.
 */
public class RoleCheck extends AbstractConsistencyCheck {

	@Override
	public String getName() {
		return "roles";
	}

	@Override
	public ConsistencyCheckResult invoke(Database db, Tx tx, boolean attemptRepair) {
		return processForType(db, RoleImpl.class, (role, result) -> {
			checkRole(role, result);
		}, attemptRepair, tx);
	}
	
	private void checkRole(Role role, ConsistencyCheckResult result) {
		String uuid = role.getUuid();

		// checkOut(role, HAS_CREATOR, UserImpl.class, response, MEDIUM);
		// checkOut(role, HAS_EDITOR, UserImpl.class, response, MEDIUM);

		if (isEmpty(role.getName())) {
			result.addInconsistency("Role name is not set or empty", uuid, HIGH);
		}
		if (role.getCreationTimestamp() == null) {
			result.addInconsistency("The role creation date is not set", uuid, MEDIUM);
		}
		if (role.getLastEditedTimestamp() == null) {
			result.addInconsistency("The role edit timestamp is not set", uuid, MEDIUM);
		}
		if (role.getBucketId() == null) {
			result.addInconsistency("The role bucket id is not set", uuid, MEDIUM);
		}
	}

}
