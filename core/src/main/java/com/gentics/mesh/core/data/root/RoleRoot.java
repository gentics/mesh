package com.gentics.mesh.core.data.root;

import com.gentics.mesh.core.data.Role;
import com.gentics.mesh.core.rest.role.RoleResponse;

public interface RoleRoot extends RootVertex<Role, RoleResponse> {

	Role create(String name);

	void addRole(Role role);

	void removeRole(Role role);

}
