package com.gentics.mesh.core.data.root;

import com.gentics.mesh.core.data.Group;
import com.gentics.mesh.core.data.Role;
import com.gentics.mesh.core.data.User;

/**
 * Aggregation node for roles.
 */
public interface RoleRoot extends RootVertex<Role> {

	/**
	 * Create a new role with the given name.
	 * 
	 * @param name
	 *            Name of the new role.
	 * @param group
	 * @param creator
	 * @return
	 */
	Role create(String name, Group group, User creator);

	void addRole(Role role);

	void removeRole(Role role);

}
