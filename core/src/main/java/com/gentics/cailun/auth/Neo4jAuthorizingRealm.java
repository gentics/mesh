package com.gentics.cailun.auth;

import io.vertx.core.logging.Logger;
import io.vertx.core.logging.impl.LoggerFactory;

import org.apache.shiro.authc.AuthenticationException;
import org.apache.shiro.authc.AuthenticationInfo;
import org.apache.shiro.authc.AuthenticationToken;
import org.apache.shiro.authc.IncorrectCredentialsException;
import org.apache.shiro.authc.SimpleAuthenticationInfo;
import org.apache.shiro.authc.UsernamePasswordToken;
import org.apache.shiro.authz.AuthorizationInfo;
import org.apache.shiro.authz.Permission;
import org.apache.shiro.authz.SimpleAuthorizationInfo;
import org.apache.shiro.realm.AuthorizingRealm;
import org.apache.shiro.subject.PrincipalCollection;
import org.springframework.beans.factory.annotation.Autowired;

import com.gentics.cailun.core.data.model.auth.CaiLunPermission;
import com.gentics.cailun.core.data.model.auth.User;
import com.gentics.cailun.core.data.service.UserService;
import com.gentics.cailun.etc.CaiLunSpringConfiguration;

public class Neo4jAuthorizingRealm extends AuthorizingRealm {

	private static final Logger log = LoggerFactory.getLogger(Neo4jAuthorizingRealm.class);

	@Autowired
	private CaiLunSpringConfiguration securityConfig;

	@Autowired
	private UserService userService;

	@Override
	protected AuthorizationInfo doGetAuthorizationInfo(PrincipalCollection principals) {
		return new SimpleAuthorizationInfo();
	}

	private long getNodeIdFromPrincipalId(String id) {
		final String nodeIdStr = id.substring(id.lastIndexOf('#') + 1);
		return Long.valueOf(nodeIdStr);
	}

	public boolean isPermitted(PrincipalCollection principals, Permission permission) {
		if (permission instanceof CaiLunPermission) {
			CaiLunPermission basicPermission = (CaiLunPermission) permission;
			try {
				long userId = getNodeIdFromPrincipalId(principals.getPrimaryPrincipal().toString());
				return userService.isPermitted(userId, basicPermission);
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		return false;
	}

	@Override
	protected AuthenticationInfo doGetAuthenticationInfo(AuthenticationToken token) throws AuthenticationException {
		UsernamePasswordToken upat = (UsernamePasswordToken) token;
		User user = userService.findByUsername(upat.getUsername());
		if (user != null) {
			return new SimpleAuthenticationInfo(user, new BCryptPasswordHash(user.getPasswordHash(), securityConfig), getName());
		} else {
			if (log.isDebugEnabled()) {
				log.debug("Could not load user with username {" + upat.getUsername() + "}.");
			}
			// TODO Don't let the user know that we know that he did not exist
			throw new IncorrectCredentialsException("Invalid credentials!");
		}
	}
}
