package com.gentics.mesh.plugin.auth;

/**
 * Filter function which will be called for every group of the mapped user.
 */
@FunctionalInterface
public interface GroupFilter {

	/**
	 * Filter for user groups.
	 * 
	 * @param groupName
	 *            Name of the group in which the user currently is part of
	 * @return true, the user will be removed from the given group. Otherwise not
	 */
	boolean filter(String groupName);
}
