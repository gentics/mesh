package com.gentics.mesh.core.endpoint.admin.consistency.check;

import static com.gentics.mesh.core.data.relationship.GraphRelationships.HAS_USER;
import static com.gentics.mesh.core.rest.admin.consistency.InconsistencySeverity.HIGH;
import static com.gentics.mesh.core.rest.admin.consistency.InconsistencySeverity.MEDIUM;
import static org.apache.commons.lang3.StringUtils.isEmpty;

import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

import com.gentics.mesh.core.data.Tx;
import com.gentics.mesh.core.data.User;
import com.gentics.mesh.core.data.dao.GroupDao;
import com.gentics.mesh.core.data.impl.UserImpl;
import com.gentics.mesh.core.data.role.HibRole;
import com.gentics.mesh.core.data.root.impl.UserRootImpl;
import com.gentics.mesh.core.endpoint.admin.consistency.AbstractConsistencyCheck;
import com.gentics.mesh.core.endpoint.admin.consistency.ConsistencyCheckResult;
import com.gentics.mesh.graphdb.spi.Database;
import com.google.common.collect.Sets;

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
		if (user.getBucketId() == null) {
			result.addInconsistency("The user bucket id is not set", uuid, MEDIUM);
		}

		assertShortcutRoleEdges(user, result);
	}

	private void assertShortcutRoleEdges(User user, ConsistencyCheckResult result) {
		GroupDao groupDao = Tx.get().groupDao();
		String uuid = user.getUuid();
		Set<HibRole> roles = user.getGroups().stream()
			.flatMap(g -> groupDao.getRoles(g).stream())
			.collect(Collectors.toSet());
		Set<HibRole> shortCutRoles = new HashSet<>();

		for (HibRole role : user.getRolesViaShortcut()) {
			shortCutRoles.add(role);
		}

		String missingShortcutMsg = "The user's shortcut role edges do not match up with the currently configured "
			+ "groups/roles. Missing shortcut role {%s}";
		String extraShortcutMsg = "Found shortcut role edge for role {%s} which should not exist for the user.";

		for (HibRole role : Sets.difference(roles, shortCutRoles)) {
			result.addInconsistency(String.format(missingShortcutMsg, role.getUuid()), uuid, HIGH);
		}

		for (HibRole role : Sets.difference(shortCutRoles, roles)) {
			result.addInconsistency(String.format(extraShortcutMsg, role.getUuid()), uuid, HIGH);
		}
	}

}
