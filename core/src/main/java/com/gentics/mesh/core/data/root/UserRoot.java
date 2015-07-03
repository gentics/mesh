package com.gentics.mesh.core.data.root;

import java.util.List;

import com.gentics.mesh.core.data.User;
import com.gentics.mesh.core.data.MeshVertex;
import com.gentics.mesh.core.data.root.impl.UserRootImpl;

public interface UserRoot extends MeshVertex {

	User create(String username);

	List<? extends User> getUsers();

	UserRootImpl getImpl();

}
