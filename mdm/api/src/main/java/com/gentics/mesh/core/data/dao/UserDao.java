package com.gentics.mesh.core.data.dao;

import com.gentics.mesh.context.InternalActionContext;
import com.gentics.mesh.core.data.user.HibUser;

/**
 * DAO for user operations.
 */
public interface UserDao {

	/**
	 * Return the sub etag for the given user.
	 * 
	 * @param user
	 * @param ac
	 * @return
	 */
	String getSubETag(HibUser user, InternalActionContext ac);

}
