package com.gentics.mesh.plugin.auth;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import com.gentics.mesh.core.rest.group.GroupResponse;
import com.gentics.mesh.core.rest.role.RoleResponse;

/**
 * Utilities which can be used for authentication plugins.
 */
public final class AuthServicePluginUtils {

	private AuthServicePluginUtils() {

	}

	/**
	 * Create a role filter which will only accept role/group combinations that are listed in the given response elements.
	 * 
	 * @param roles
	 *            List of roles which contain references to assigned groups
	 * @param groups
	 *            List of groups which contain references to assigned roles
	 * @return Created role filter
	 */
	public static RoleFilter createRoleFilter(List<RoleResponse> roles, List<GroupResponse> groups) {
		Set<String> allowed = new HashSet<>();
		groups.forEach(g -> {
			g.getRoles().forEach(r -> {
				allowed.add(r.getName() + "-" + g.getName());
			});
		});
		roles.forEach(r -> {
			r.getGroups().forEach(g -> {
				allowed.add(r.getName() + "-" + g.getName());
			});
		});
		return (groupName, roleName) -> {
			return !allowed.contains(roleName + "-" + groupName);
		};
	}

	/**
	 * Create a group filter which will only accept groups for the user that are part of the given list.
	 * 
	 * @param groups
	 *            List of groups to which the user should belong
	 * @return Created group filter
	 */
	public static GroupFilter createGroupFilter(List<GroupResponse> groups) {
		List<String> allowed = new ArrayList<>();
		allowed.addAll(groups.stream().map(GroupResponse::getName).collect(Collectors.toList()));
		return (groupName) -> {
			return !allowed.contains(groupName);
		};
	}
}
