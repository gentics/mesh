package com.gentics.cailun.auth;

import io.vertx.ext.auth.impl.realms.ShiroAuthRealmImpl;
import io.vertx.ext.auth.impl.realms.SimplePrincipalCollection;

import org.apache.shiro.authz.AuthorizationException;
import org.apache.shiro.mgt.SecurityManager;
import org.apache.shiro.realm.Realm;
import org.apache.shiro.subject.PrincipalCollection;
import org.apache.shiro.subject.Subject;
import org.apache.shiro.subject.SubjectContext;
import org.apache.shiro.subject.support.DefaultSubjectContext;

import com.gentics.cailun.core.rest.model.GenericPermission;

/**
 * Enhanced version of the default shiro authrealm implementation that exposed some protected fields.
 * 
 * @author johannes2
 *
 */
public class EnhancedShiroAuthRealmImpl extends ShiroAuthRealmImpl {

	public EnhancedShiroAuthRealmImpl(Realm realm) {
		super(realm);
	}

	public SecurityManager getSecurityManager() {
		return this.securityManager;
	}

	public boolean hasPermission(String principal, GenericPermission permission) {
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
