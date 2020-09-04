package com.gentics.mesh.core.data.user;

import io.vertx.core.shareddata.impl.ClusterSerializable;
import io.vertx.ext.auth.User;

/**
 * Mesh graph user which additionally implements the vertex {@link User} interface.
 */
public interface MeshAuthUser extends User, HibUser, ClusterSerializable {

	HibUser getDelegate();

}
