package com.gentics.mesh.core.verticle.admin.consistency.asserter;

import static com.gentics.mesh.core.data.relationship.GraphRelationships.HAS_USER;
import static com.gentics.mesh.core.rest.admin.consistency.InconsistencySeverity.HIGH;
import static com.gentics.mesh.core.rest.admin.consistency.InconsistencySeverity.MEDIUM;
import static org.apache.commons.lang3.StringUtils.isEmpty;

import java.util.Iterator;

import com.gentics.mesh.core.data.User;
import com.gentics.mesh.core.data.impl.UserImpl;
import com.gentics.mesh.core.data.root.impl.UserRootImpl;
import com.gentics.mesh.core.rest.admin.consistency.ConsistencyCheckResponse;
import com.gentics.mesh.core.verticle.admin.consistency.ConsistencyCheck;
import com.gentics.mesh.graphdb.spi.Database;

/**
 * User specific checks.
 */
public class UserCheck implements ConsistencyCheck {

	@Override
	public void invoke(Database db, ConsistencyCheckResponse response) {
		Iterator<? extends User> it = db.getVerticesForType(UserImpl.class);
		while (it.hasNext()) {
			checkUser(it.next(), response);
		}
	}

	private void checkUser(User user, ConsistencyCheckResponse response) {
		String uuid = user.getUuid();

		checkIn(user, HAS_USER, UserRootImpl.class, response, HIGH);
		//checkOut(user, HAS_CREATOR, UserImpl.class, response, MEDIUM);
		//checkOut(user, HAS_EDITOR, UserImpl.class, response, MEDIUM);

		if (isEmpty(user.getUsername())) {
			response.addInconsistency("Username is empty or not set", uuid, HIGH);
		}
		if (user.getCreationTimestamp() == null) {
			response.addInconsistency("The user creation date is not set", uuid, MEDIUM);
		}
		if (user.getLastEditedTimestamp() == null) {
			response.addInconsistency("The user edit timestamp is not set", uuid, MEDIUM);
		}
	}

}
