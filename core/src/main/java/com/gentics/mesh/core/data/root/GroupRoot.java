package com.gentics.mesh.core.data.root;

import java.util.List;

import com.gentics.mesh.core.data.Group;
import com.gentics.mesh.core.data.MeshVertex;
import com.gentics.mesh.core.data.root.impl.GroupRootImpl;

public interface GroupRoot extends MeshVertex {

	Group create(String name);

	List<? extends Group> getGroups();

	GroupRootImpl getImpl();
}
