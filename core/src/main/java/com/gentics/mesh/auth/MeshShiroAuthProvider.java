package com.gentics.mesh.auth;

import io.vertx.core.AsyncResult;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.VertxException;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.auth.User;
import io.vertx.ext.auth.shiro.ShiroAuth;
import io.vertx.ext.auth.shiro.ShiroAuthRealmType;
import io.vertx.ext.auth.shiro.impl.LDAPAuthProvider;
import io.vertx.ext.auth.shiro.impl.PropertiesAuthProvider;
import io.vertx.ext.auth.shiro.impl.ShiroAuthProviderImpl;

import org.apache.shiro.authc.AuthenticationException;
import org.apache.shiro.authc.AuthenticationToken;
import org.apache.shiro.authc.UsernamePasswordToken;
import org.apache.shiro.mgt.DefaultSecurityManager;
import org.apache.shiro.realm.Realm;
import org.apache.shiro.subject.Subject;
import org.apache.shiro.subject.SubjectContext;
import org.apache.shiro.subject.support.DefaultSubjectContext;

import com.gentics.mesh.core.data.model.tinkerpop.MeshShiroUser;

public class MeshShiroAuthProvider implements ShiroAuth {

	private Vertx vertx;
	private org.apache.shiro.mgt.SecurityManager securityManager;
	private String rolePrefix = DEFAULT_ROLE_PREFIX;

	public static ShiroAuth create(Vertx vertx, ShiroAuthRealmType realmType, JsonObject config) {
		Realm realm;
		switch (realmType) {
		case PROPERTIES:
			realm = PropertiesAuthProvider.createRealm(config);
			break;
		case LDAP:
			realm = LDAPAuthProvider.createRealm(config);
			break;
		default:
			throw new IllegalArgumentException("Invalid shiro auth realm type: " + realmType);
		}
		return new ShiroAuthProviderImpl(vertx, realm);
	}

	public MeshShiroAuthProvider(Vertx vertx, Realm realm) {
		this.vertx = vertx;
		this.securityManager = new DefaultSecurityManager(realm);
	}

	@Override
	public void authenticate(JsonObject authInfo, Handler<AsyncResult<User>> resultHandler) {
		vertx.executeBlocking(fut -> {
			SubjectContext subjectContext = new DefaultSubjectContext();
			Subject subject = securityManager.createSubject(subjectContext);
			String username = authInfo.getString("username");
			String password = authInfo.getString("password");
			AuthenticationToken token = new UsernamePasswordToken(username, password);
			try {
				subject.login(token);
			} catch (AuthenticationException e) {
				throw new VertxException(e);
			}
			System.out.println("Class: " + subject.getPrincipal().getClass().getName());
			fut.complete(new MeshShiroUser(vertx, securityManager, username, rolePrefix));
		}, resultHandler);
	}

	@Override
	public ShiroAuth setRolePrefix(String rolePrefix) {
		this.rolePrefix = rolePrefix;
		return this;
	}

	Vertx getVertx() {
		return vertx;
	}

	org.apache.shiro.mgt.SecurityManager getSecurityManager() {
		return securityManager;
	}

}
