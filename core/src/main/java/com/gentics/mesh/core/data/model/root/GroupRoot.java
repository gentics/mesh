package com.gentics.mesh.core.data.model.root;

import java.util.List;

import com.gentics.mesh.core.data.model.Group;
import com.gentics.mesh.core.data.model.MeshVertex;
import com.gentics.mesh.core.data.model.root.impl.GroupRootImpl;

public interface GroupRoot extends MeshVertex {

	Group create(String name);

	List<? extends Group> getGroups();

	GroupRootImpl getImpl();
}
