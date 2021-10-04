package com.gentics.mesh.core.data.dao;

import com.gentics.mesh.core.data.user.HibUser;

/**
 * User DAO
 */
public interface UserDaoWrapper extends UserDao, OrientDBDaoGlobal<HibUser> {

}
