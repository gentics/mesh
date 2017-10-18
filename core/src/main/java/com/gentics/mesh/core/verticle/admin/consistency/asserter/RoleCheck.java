package com.gentics.mesh.core.verticle.admin.consistency.asserter;

import static com.gentics.mesh.core.rest.admin.consistency.InconsistencySeverity.HIGH;
import static com.gentics.mesh.core.rest.admin.consistency.InconsistencySeverity.MEDIUM;
import static org.apache.commons.lang3.StringUtils.isEmpty;

import com.gentics.mesh.cli.BootstrapInitializer;
import com.gentics.mesh.core.data.Role;
import com.gentics.mesh.core.rest.admin.consistency.ConsistencyCheckResponse;
import com.gentics.mesh.core.verticle.admin.consistency.ConsistencyCheck;

/**
 * Role specific consistency checks.
 */
public class RoleCheck implements ConsistencyCheck {

	@Override
	public void invoke(BootstrapInitializer boot, ConsistencyCheckResponse response) {
		for (Role role : boot.roleRoot().findAllIt()) {
			checkRole(role, response);
		}
	}

	private void checkRole(Role role, ConsistencyCheckResponse response) {
		String uuid = role.getUuid();

		if (isEmpty(role.getName())) {
			response.addInconsistency("Role name is not set or empty", uuid, HIGH);
		}
		if (role.getCreationTimestamp() == null) {
			response.addInconsistency("The role creation date is not set", uuid, MEDIUM);
		}
		if (role.getCreator() == null) {
			response.addInconsistency("The role creator is not set", uuid, MEDIUM);
		}
		if (role.getEditor() == null) {
			response.addInconsistency("The role editor is not set", uuid, MEDIUM);
		}
		if (role.getLastEditedTimestamp() == null) {
			response.addInconsistency("The role edit timestamp is not set", uuid, MEDIUM);
		}
	}

}
