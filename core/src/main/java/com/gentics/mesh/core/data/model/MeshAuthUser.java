package com.gentics.mesh.core.data.model;

import io.vertx.core.AsyncResult;
import io.vertx.core.Handler;
import io.vertx.ext.auth.User;

import com.gentics.mesh.core.data.model.impl.MeshAuthUserImpl;
import com.gentics.mesh.core.data.model.relationship.Permission;

public interface MeshAuthUser extends MeshUser, User {

	MeshAuthUser isAuthorised(MeshVertex targetNode, Permission permission, Handler<AsyncResult<Boolean>> resultHandler);
	
	MeshAuthUserImpl getImpl();

}
