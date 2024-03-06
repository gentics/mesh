package com.gentics.mesh.core.data.root;

import com.gentics.mesh.core.data.Group;
import com.gentics.mesh.core.data.Role;
import com.gentics.mesh.core.data.page.Page;
import com.gentics.mesh.core.data.user.HibUser;
import com.gentics.mesh.core.rest.role.RoleResponse;
import com.gentics.mesh.parameter.PagingParameters;

/**
 * Aggregation node for roles.
 */
public interface RoleRoot extends RootVertex<Role>, TransformableElementRoot<Role, RoleResponse> {

	/**
	 * Return the page of groups for the given role.
	 * 
	 * @param role
	 *            Role to load groups from
	 * @param user
	 *            User to be used to check read permissions
	 * @param pagingInfo
	 *            Paging parameters
	 * @return Return the page of groups
	 */
	Page<? extends Group> getGroups(Role role, HibUser user, PagingParameters pagingInfo);

	/**
	 * Add the role.
	 * 
	 * @param role
	 */
	void addRole(Role role);

	/**
	 * Remove the role.
	 * 
	 * @param role
	 */
	void removeRole(Role role);

	/**
	 * Create a shallow entity.
	 * 
	 * @return
	 */
	Role create();

}
