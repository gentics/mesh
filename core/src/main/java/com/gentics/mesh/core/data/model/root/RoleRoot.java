package com.gentics.mesh.core.data.model.root;

import java.util.List;

import com.gentics.mesh.core.data.model.MeshVertex;
import com.gentics.mesh.core.data.model.Role;
import com.gentics.mesh.core.data.model.root.impl.RoleRootImpl;

public interface RoleRoot extends MeshVertex {

	Role create(String name);

	List<? extends Role> getRoles();

	RoleRootImpl getImpl();

}
