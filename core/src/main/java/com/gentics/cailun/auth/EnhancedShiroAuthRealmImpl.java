package com.gentics.cailun.auth;

import io.vertx.ext.auth.shiro.impl.ShiroAuthRealmBase;
import io.vertx.ext.auth.shiro.impl.SimplePrincipalCollection;

import org.apache.shiro.authz.AuthorizationException;
import org.apache.shiro.authz.Permission;
import org.apache.shiro.mgt.DefaultSecurityManager;
import org.apache.shiro.realm.Realm;
import org.apache.shiro.subject.PrincipalCollection;
import org.apache.shiro.subject.Subject;
import org.apache.shiro.subject.SubjectContext;
import org.apache.shiro.subject.support.DefaultSubjectContext;

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

	public boolean hasPermission(String principal, Permission permission) {
		SubjectContext subjectContext = new DefaultSubjectContext();
		PrincipalCollection coll = new SimplePrincipalCollection(principal);
		subjectContext.setPrincipals(coll);
		Subject subject = securityManager.createSubject(subjectContext);
		try {
			subject.checkPermission(permission);
			return true;
		} catch (AuthorizationException e) {
			return false;
		}
	}

}
