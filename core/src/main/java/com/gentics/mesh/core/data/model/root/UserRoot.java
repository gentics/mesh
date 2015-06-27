package com.gentics.mesh.core.data.model.root;

import java.util.List;

import com.gentics.mesh.core.data.model.MeshUser;
import com.gentics.mesh.core.data.model.MeshVertex;
import com.gentics.mesh.core.data.model.root.impl.UserRootImpl;

public interface UserRoot extends MeshVertex {

	MeshUser create(String username);

	List<? extends MeshUser> getUsers();
	
	UserRootImpl getImpl();


}
