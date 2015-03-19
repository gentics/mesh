package com.gentics.cailun.auth;

import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.auth.impl.AuthServiceImpl;
import io.vertx.ext.auth.impl.LoginSession;

import org.apache.shiro.authz.Permission;

import com.gentics.cailun.core.data.model.auth.User;

/**
 * Custom auth* service implementation that adds support for object based permission checks
 * 
 * @author johannes2
 *
 */
public class CaiLunAuthServiceImpl extends AuthServiceImpl {

	private Vertx vertx;
	private final ExposingShiroAuthProvider provider;

	public CaiLunAuthServiceImpl(Vertx vertx, ExposingShiroAuthProvider provider, JsonObject config) {
		super(vertx, config, provider);
		this.vertx = vertx;
		this.provider = provider;
	}

	// public void hasPermission(String loginID, Permission permission, Handler<AsyncResult<Boolean>> resultHandler) {
	// vertx.executeBlocking((Future<Boolean> fut) -> {
	// fut.complete(hasPermission(loginID, permission));
	// }, resultHandler);
	// }

	public boolean hasPermission(String loginID, Permission permission) {

		LoginSession session = loginSessions.get(loginID);
		if (session != null) {
			if (session.principal() != null && session.principal() instanceof User) {
				User user = (User) session.principal();
				boolean hasPerm = getAuthRealm().hasPermission(user.getPrincipalId(), permission);
				return hasPerm;
			}
			return false;
			// doHasPermission(session, permission, resultHandler);
		}
		return false;

	}

	public EnhancedShiroAuthRealmImpl getAuthRealm() {
		return (EnhancedShiroAuthRealmImpl) provider.getRealm();
	}

}
