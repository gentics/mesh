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

	public static final String TYPE = "roles";

	Page<? extends Group> getGroups(Role role, HibUser user, PagingParameters pagingInfo);

	void addRole(Role role);

	void removeRole(Role role);

	/**
	 * Create a shallow entity.
	 * @return
	 */
	Role create();


}
