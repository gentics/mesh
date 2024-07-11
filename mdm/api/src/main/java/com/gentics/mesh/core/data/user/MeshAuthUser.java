package com.gentics.mesh.core.data.user;

import io.vertx.core.shareddata.impl.ClusterSerializable;

/**
 * Mesh graph user which additionally implements the vertex {@link User} interface.
 */
public interface MeshAuthUser extends io.vertx.ext.auth.User, ClusterSerializable {

	/**
	 * Return the delegate which references the database entity of the auth user.
	 * 
	 * @return
	 */
	User getDelegate();
}
