package com.gentics.mesh.core.data.dao;

import com.gentics.mesh.context.InternalActionContext;
import com.gentics.mesh.core.data.User;
import com.gentics.mesh.core.data.root.UserRoot;
import com.gentics.mesh.madl.traversal.TraversalResult;

// TODO move the contents of this to UserDao once migration is done
public interface UserDaoWrapper extends UserDao, UserRoot, DaoWrapper<User> {

	String getSubETag(User user, InternalActionContext ac);

	TraversalResult<? extends User> findAll();
}
