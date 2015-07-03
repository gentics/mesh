package com.gentics.mesh.core.data.root;

import com.gentics.mesh.core.Page;
import com.gentics.mesh.core.data.Group;
import com.gentics.mesh.core.data.MeshAuthUser;
import com.gentics.mesh.core.data.root.impl.GroupRootImpl;
import com.gentics.mesh.paging.PagingInfo;
import com.gentics.mesh.util.InvalidArgumentException;

public interface GroupRoot extends RootVertex<Group> {

	Group create(String name);

	GroupRootImpl getImpl();

	Page<? extends Group> findAll(MeshAuthUser requestUser, PagingInfo pagingInfo) throws InvalidArgumentException;

	void addGroup(Group group);
}
