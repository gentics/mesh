package com.gentics.mesh.core.data.root;

import com.gentics.mesh.core.Page;
import com.gentics.mesh.core.data.MeshAuthUser;
import com.gentics.mesh.core.data.Role;
import com.gentics.mesh.core.data.root.impl.RoleRootImpl;
import com.gentics.mesh.paging.PagingInfo;
import com.gentics.mesh.util.InvalidArgumentException;

public interface RoleRoot extends RootVertex<Role> {

	Role create(String name);

	RoleRootImpl getImpl();

	Page<? extends Role> findAll(MeshAuthUser requestUser, PagingInfo pagingInfo) throws InvalidArgumentException;

	void addRole(Role role);

	void removeRole(Role role);

}
