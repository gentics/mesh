package com.gentics.mesh.auth;

import io.vertx.core.Vertx;
import io.vertx.ext.auth.shiro.ShiroAuthRealm;
import io.vertx.ext.auth.shiro.impl.ShiroAuthProviderImpl;

public class ExposingShiroAuthProvider extends ShiroAuthProviderImpl {

	protected ShiroAuthRealm realm;

	public ExposingShiroAuthProvider(Vertx vertx, ShiroAuthRealm realm) {
		super(vertx, realm);
		this.realm = realm;
	}

	public ShiroAuthRealm getRealm() {
		return realm;
	}

}
