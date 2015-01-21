package com.gentics.vertx.cailun.auth;

import io.vertx.ext.auth.impl.realms.ShiroAuthRealmImpl;

import org.apache.shiro.mgt.SecurityManager;
import org.apache.shiro.realm.Realm;

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

}
