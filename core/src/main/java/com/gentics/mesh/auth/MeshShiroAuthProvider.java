package com.gentics.mesh.auth;

import io.vertx.core.Vertx;
import io.vertx.ext.auth.AuthProvider;
import io.vertx.ext.auth.shiro.impl.ShiroAuthProviderImpl;

import org.apache.shiro.realm.Realm;

public class MeshShiroAuthProvider extends ShiroAuthProviderImpl implements AuthProvider {

	public MeshShiroAuthProvider(Vertx vertx, Realm realm) {
		super(vertx, realm);
	}

}
