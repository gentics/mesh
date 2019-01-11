package com.gentics.mesh.core.endpoint.admin.consistency.check;

import static com.gentics.mesh.core.data.relationship.GraphRelationships.HAS_USER;
import static com.gentics.mesh.core.rest.admin.consistency.InconsistencySeverity.HIGH;
import static com.gentics.mesh.core.rest.admin.consistency.InconsistencySeverity.MEDIUM;
import static org.apache.commons.lang3.StringUtils.isEmpty;

import java.util.HashSet;
import java.util.Set;

import com.gentics.mesh.core.data.Group;
import com.gentics.mesh.core.data.Role;
import com.gentics.mesh.core.data.User;
import com.gentics.mesh.core.data.impl.UserImpl;
import com.gentics.mesh.core.data.root.impl.UserRootImpl;
import com.gentics.mesh.core.endpoint.admin.consistency.AbstractConsistencyCheck;
import com.gentics.mesh.core.endpoint.admin.consistency.ConsistencyCheckResult;
import com.gentics.mesh.graphdb.spi.Database;
import com.syncleus.ferma.tx.Tx;

/**
 * User specific checks.
 */
public class UserCheck extends AbstractConsistencyCheck {

	@Override
	public String getName() {
		return "users";
	}

	@Override
	public ConsistencyCheckResult invoke(Database db, Tx tx, boolean attemptRepair) {
		return processForType(db, UserImpl.class, (user, result) -> {
			checkUser(user, result);
		}, attemptRepair, tx);
	}

	private void checkUser(User user, ConsistencyCheckResult result) {
		String uuid = user.getUuid();

		checkIn(user, HAS_USER, UserRootImpl.class, result, HIGH);
		// checkOut(user, HAS_CREATOR, UserImpl.class, response, MEDIUM);
		// checkOut(user, HAS_EDITOR, UserImpl.class, response, MEDIUM);

		if (isEmpty(user.getUsername())) {
			result.addInconsistency("Username is empty or not set", uuid, HIGH);
		}
		if (user.getCreationTimestamp() == null) {
			result.addInconsistency("The user creation date is not set", uuid, MEDIUM);
		}
		if (user.getLastEditedTimestamp() == null) {
			result.addInconsistency("The user edit timestamp is not set", uuid, MEDIUM);
		}

		assertShortcutRoleEdges(user, result);
	}

	private void assertShortcutRoleEdges(User user, ConsistencyCheckResult result) {
		String uuid = user.getUuid();

		Set<Role> shortCutRoles = new HashSet<>();
		for (Role role : user.getRolesViaShortcut()) {
			shortCutRoles.add(role);
		}

		for (Group group : user.getGroups()) {
			for (Role role : group.getRoles()) {
				if (!shortCutRoles.contains(role)) {
					result.addInconsistency(
						"The user's shortcut role edges do not match up with the currently configured groups/roles. Missing role {"
							+ role.getUuid() + "}",
						uuid, HIGH);
				} else {
					shortCutRoles.remove(role);
				}
			}
		}

		for (Role role : shortCutRoles) {
			result.addInconsistency("Found shortcut role edge for role {" + role.getUuid() + "} which should not exist for the user.", uuid, HIGH);
		}

	}

}
