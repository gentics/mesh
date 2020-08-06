package com.gentics.mesh.core.data.dao;

import com.gentics.mesh.core.data.Group;
import com.gentics.mesh.madl.traversal.TraversalResult;

// TODO move the contents of this to GroupDao once migration is done
public interface GroupDaoWrapper {

	TraversalResult<? extends Group> findAll();
}
