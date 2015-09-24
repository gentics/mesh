package com.gentics.mesh.core.data;

import io.vertx.core.AsyncResult;
import io.vertx.core.Handler;
import io.vertx.ext.auth.User;

import com.gentics.mesh.core.data.impl.MeshAuthUserImpl;
import com.gentics.mesh.core.data.relationship.GraphPermission;

public interface MeshAuthUser extends User, com.gentics.mesh.core.data.User {

//	/**
//	 * Check whether one of the roles of the user grants the given permission on the target node.
//	 * 
//	 * @param targetNode Element that should be checked
//	 * @param permission Permission that should be used when checking the roles
//	 * @param resultHandler Handler that is being invoked once the permission check has been performed
//	 * @return fluent API
//	 */
//	MeshAuthUser isAuthorised(MeshVertex targetNode, GraphPermission permission, Handler<AsyncResult<Boolean>> resultHandler);

	MeshAuthUserImpl getImpl();


}
