package com.gentics.mesh.core.verticle.admin.consistency.asserter;

import static com.gentics.mesh.core.rest.admin.consistency.InconsistencySeverity.HIGH;
import static com.gentics.mesh.core.rest.admin.consistency.InconsistencySeverity.MEDIUM;
import static org.apache.commons.lang3.StringUtils.isEmpty;

import com.gentics.mesh.cli.BootstrapInitializer;
import com.gentics.mesh.core.data.User;
import com.gentics.mesh.core.rest.admin.consistency.ConsistencyCheckResponse;
import com.gentics.mesh.core.verticle.admin.consistency.ConsistencyCheck;

/**
 * User specific checks.
 */
public class UserCheck implements ConsistencyCheck {

	@Override
	public void invoke(BootstrapInitializer boot, ConsistencyCheckResponse response) {
		for (User user : boot.userRoot().findAllIt()) {
			checkUser(user, response);
		}
	}

	private void checkUser(User user, ConsistencyCheckResponse response) {
		String uuid = user.getUuid();

		if (isEmpty(user.getUsername())) {
			response.addInconsistency("Username is empty or not set", uuid, HIGH);
		}
		if (user.getCreationTimestamp() == null) {
			response.addInconsistency("The user creation date is not set", uuid, MEDIUM);
		}
		if (user.getCreator() == null) {
			response.addInconsistency("The user creator is not set", uuid, MEDIUM);
		}
		if (user.getEditor() == null) {
			response.addInconsistency("The user editor is not set", uuid, MEDIUM);
		}
		if (user.getLastEditedTimestamp() == null) {
			response.addInconsistency("The user edit timestamp is not set", uuid, MEDIUM);
		}
	}

}
