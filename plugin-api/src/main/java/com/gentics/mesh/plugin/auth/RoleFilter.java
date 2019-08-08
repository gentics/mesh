package com.gentics.mesh.plugin.auth;

/**
 * Filter function which will be called for every role of the mapped groups. It can be used to decide from which groups the user should be removed.
 */
@FunctionalInterface
public interface RoleFilter {

	/**
	 * Filter roles that are assigned to the group.
	 * 
	 * @param groupName
	 *            Name of the group
	 * @param roleName
	 *            Name of the role that currently is assigned to the group
	 * @return true, the role will be removed from the group
	 */
	boolean filter(String groupName, String roleName);
}
