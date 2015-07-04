package com.gentics.mesh.core.data.root;

import com.gentics.mesh.core.data.Role;

public interface RoleRoot extends RootVertex<Role> {

	Role create(String name);

	void addRole(Role role);

	void removeRole(Role role);

}
