package com.gentics.mesh.core.data.user;

import io.vertx.core.shareddata.impl.ClusterSerializable;
import io.vertx.ext.auth.User;

/**
 * Mesh graph user which additionally implements the vertex {@link User} interface.
 */
public interface MeshAuthUser extends User, ClusterSerializable {

	/**
	 * Return the delegate which references the database entity of the auth user.
	 * 
	 * @return
	 */
	HibUser getDelegate();

}
