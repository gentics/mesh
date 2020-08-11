package com.gentics.mesh.core.actions;

import com.gentics.mesh.core.data.user.HibUser;
import com.gentics.mesh.core.rest.user.UserResponse;
import com.gentics.mesh.core.verticle.handler.DAOActions;

public interface UserDAOActions extends DAOActions<HibUser, UserResponse> {

}
