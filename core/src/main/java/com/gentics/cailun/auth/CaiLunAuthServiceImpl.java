package com.gentics.cailun.auth;

import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.auth.impl.AuthServiceImpl;

import org.apache.shiro.authz.Permission;

/**
 * Custom auth* service implementation that adds support for object based permission checks
 * 
 * @author johannes2
 *
 */
public class CaiLunAuthServiceImpl extends AuthServiceImpl {

	public CaiLunAuthServiceImpl(Vertx vertx, EnhancedShiroAuthRealmImpl authRealm, JsonObject config) {
		super(vertx, authRealm, config);
	}

	private CaiLunAuthServiceImpl(Vertx vertx, JsonObject config) {
		super(vertx, config);
	}

	public void hasPermission(String principal, Permission permission, Handler<AsyncResult<Boolean>> resultHandler) {
		vertx.executeBlocking((Future<Boolean> fut) -> {
			fut.complete(hasPermission(principal, permission));
		}, resultHandler);
	}

	public boolean hasPermission(String principal, Permission permission) {
		boolean hasPerm = getAuthRealm().hasPermission(principal, permission);
		return hasPerm;
	}

	public EnhancedShiroAuthRealmImpl getAuthRealm() {
		return (EnhancedShiroAuthRealmImpl) realm;
	}

}
