package com.gentics.mesh.core.data.root;

import java.util.List;

import com.gentics.mesh.core.data.MeshUser;
import com.gentics.mesh.core.data.MeshVertex;
import com.gentics.mesh.core.data.root.impl.UserRootImpl;

public interface UserRoot extends MeshVertex {

	MeshUser create(String username);

	List<? extends MeshUser> getUsers();
	
	UserRootImpl getImpl();


}
