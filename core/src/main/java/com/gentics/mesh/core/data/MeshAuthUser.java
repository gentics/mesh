package com.gentics.mesh.core.data;

import io.vertx.core.AsyncResult;
import io.vertx.core.Handler;
import io.vertx.ext.auth.User;

import com.gentics.mesh.core.data.impl.MeshAuthUserImpl;
import com.gentics.mesh.core.data.relationship.Permission;

public interface MeshAuthUser extends User, com.gentics.mesh.core.data.User {

	MeshAuthUser isAuthorised(MeshVertex targetNode, Permission permission, Handler<AsyncResult<Boolean>> resultHandler);
	
	MeshAuthUserImpl getImpl();

}
