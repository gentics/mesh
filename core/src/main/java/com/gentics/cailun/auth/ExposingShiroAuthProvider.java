package com.gentics.cailun.auth;

import io.vertx.core.Vertx;
import io.vertx.ext.auth.shiro.impl.ShiroAuthProvider;
import io.vertx.ext.auth.shiro.impl.ShiroAuthRealm;

public class ExposingShiroAuthProvider extends ShiroAuthProvider {

	protected ShiroAuthRealm realm;

	public ExposingShiroAuthProvider(Vertx vertx, ShiroAuthRealm realm) {
		super(vertx, realm);
		this.realm = realm;
	}

	public ShiroAuthRealm getRealm() {
		return realm;
	}

}
