package com.gentics.mesh.core.data.root;

import com.gentics.mesh.core.data.Role;
import com.gentics.mesh.core.data.User;

/**
 * Aggregation node for roles.
 */
public interface RoleRoot extends RootVertex<Role> {

	public static final String TYPE = "roles";

	/**
	 * Create a new role with the given name.
	 * 
	 * @param name
	 *            Name of the new role.
	 * @param creator
	 *            User that is being used to set the reference fields
	 * @return Created role
	 */
	Role create(String name, User creator);

	/**
	 * Add the given role to this aggregation vertex.
	 * 
	 * @param role
	 *            Role to be added
	 */
	void addRole(Role role);

	/**
	 * Remove the given role from this aggregation vertex.
	 * 
	 * @param role
	 *            Role to be removed
	 */
	void removeRole(Role role);

}
