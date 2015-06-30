package com.gentics.mesh.core.data.root;

import java.util.List;

import com.gentics.mesh.core.data.MeshVertex;
import com.gentics.mesh.core.data.Role;
import com.gentics.mesh.core.data.root.impl.RoleRootImpl;

public interface RoleRoot extends MeshVertex {

	Role create(String name);

	List<? extends Role> getRoles();

	RoleRootImpl getImpl();

}
