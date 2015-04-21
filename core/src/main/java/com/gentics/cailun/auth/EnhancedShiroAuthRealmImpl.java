package com.gentics.cailun.auth;

import io.vertx.core.VertxException;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.auth.shiro.impl.ShiroAuthRealmBase;
import io.vertx.ext.auth.shiro.impl.SimplePrincipalCollection;

import org.apache.shiro.authc.AuthenticationException;
import org.apache.shiro.authc.AuthenticationToken;
import org.apache.shiro.authc.UsernamePasswordToken;
import org.apache.shiro.authz.AuthorizationException;
import org.apache.shiro.mgt.DefaultSecurityManager;
import org.apache.shiro.realm.Realm;
import org.apache.shiro.subject.PrincipalCollection;
import org.apache.shiro.subject.Subject;
import org.apache.shiro.subject.SubjectContext;
import org.apache.shiro.subject.support.DefaultSubjectContext;

import com.gentics.cailun.core.data.model.auth.User;

/**
 * Enhanced version of the default shiro authrealm implementation that exposed some protected fields.
 * 
 * @author johannes2
 *
 */
public class EnhancedShiroAuthRealmImpl extends ShiroAuthRealmBase {

	public EnhancedShiroAuthRealmImpl(Realm realm) {
		super(realm);
	}

	public DefaultSecurityManager getSecurityManager() {
		return this.securityManager;
	}

	@Override
	public void login(JsonObject principal, JsonObject credentials) {
		SubjectContext subjectContext = new DefaultSubjectContext();
		Subject subject = securityManager.createSubject(subjectContext);
		String username = principal.getString("username");
		String password = credentials.getString("password");
		AuthenticationToken token = new UsernamePasswordToken(username, password);
		try {
			subject.login(token);
			if (subject.getPrincipal() instanceof User) {
				User user = (User) subject.getPrincipal();
				principal.put("uuid", user.getUuid());
				principal.put("id", user.getId());
			}
		} catch (AuthenticationException e) {
			throw new VertxException(e);
		}
	}

	@Override
	public boolean hasPermission(JsonObject principal, String permission) {
		SubjectContext subjectContext = new DefaultSubjectContext();
		Long uid = principal.getLong("id");
		PrincipalCollection coll = new SimplePrincipalCollection(uid);
		subjectContext.setPrincipals(coll);
		Subject subject = securityManager.createSubject(subjectContext);
		try {
			subject.checkPermission(permission);
			return true;
		} catch (AuthorizationException e) {
			return false;
		}
	}

	// public boolean hasPermission(String principal, Permission permission) {
	// SubjectContext subjectContext = new DefaultSubjectContext();
	// PrincipalCollection coll = new SimplePrincipalCollection(principal);
	// subjectContext.setPrincipals(coll);
	// Subject subject = securityManager.createSubject(subjectContext);
	// try {
	// subject.checkPermission(permission);
	// return true;
	// } catch (AuthorizationException e) {
	// return false;
	// }
	// }

}
