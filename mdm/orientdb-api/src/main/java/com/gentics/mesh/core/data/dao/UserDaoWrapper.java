package com.gentics.mesh.core.data.dao;

import com.gentics.mesh.core.data.User;
import com.gentics.mesh.madl.traversal.TraversalResult;

// TODO move the contents of this to UserDao once migration is done
public interface UserDaoWrapper {

	TraversalResult<? extends User> findAll();
}
