package com.gentics.mesh.core.data;

import io.vertx.ext.auth.User;

import com.gentics.mesh.core.data.impl.MeshAuthUserImpl;

public interface MeshAuthUser extends User, com.gentics.mesh.core.data.User {

	MeshAuthUserImpl getImpl();

}
