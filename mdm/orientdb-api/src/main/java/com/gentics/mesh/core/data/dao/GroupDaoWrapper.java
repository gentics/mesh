package com.gentics.mesh.core.data.dao;

import com.gentics.mesh.core.data.Group;
import com.gentics.mesh.core.data.root.GroupRoot;
import com.gentics.mesh.madl.traversal.TraversalResult;

// TODO move the contents of this to GroupDao once migration is done
public interface GroupDaoWrapper extends GroupDao, GroupRoot {

	TraversalResult<? extends Group> findAll();
}
