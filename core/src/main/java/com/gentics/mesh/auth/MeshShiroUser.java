package com.gentics.mesh.auth;

import io.vertx.core.AsyncResult;
import io.vertx.core.Handler;
import io.vertx.core.json.JsonObject;
import io.vertx.core.shareddata.impl.ClusterSerializable;
import io.vertx.ext.auth.AbstractUser;
import io.vertx.ext.auth.AuthProvider;

public class MeshShiroUser extends AbstractUser implements ClusterSerializable {

	@Override
	public JsonObject principal() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void setAuthProvider(AuthProvider authProvider) {
		// TODO Auto-generated method stub

	}

	@Override
	protected void doIsPermitted(String permission, Handler<AsyncResult<Boolean>> resultHandler) {
		// TODO Auto-generated method stub

	}

}
