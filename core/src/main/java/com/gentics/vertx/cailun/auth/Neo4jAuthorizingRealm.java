package com.gentics.vertx.cailun.auth;

import java.util.HashSet;
import java.util.Set;

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

import com.gentics.vertx.cailun.perm.GroupRepository;
import com.gentics.vertx.cailun.perm.RoleRepository;
import com.gentics.vertx.cailun.perm.UserRepository;
import com.gentics.vertx.cailun.perm.model.User;

public class Neo4jAuthorizingRealm extends AuthorizingRealm {

	@Autowired
	SecurityConfiguration securityConfig;

	@Autowired
	UserRepository userRepository;

	@Autowired
	GroupRepository groupRepository;

	@Autowired
	RoleRepository RoleRepository;
	
	@Override
	protected AuthorizationInfo doGetAuthorizationInfo(PrincipalCollection principals) {

		Set<String> roles = new HashSet<>();
		Set<Permission> permissions = new HashSet<>();

		SimpleAuthorizationInfo info = new SimpleAuthorizationInfo(roles);
		info.setRoles(roles);
		info.setObjectPermissions(permissions);
		return info;
	}

	@Override
	protected AuthenticationInfo doGetAuthenticationInfo(AuthenticationToken token) throws AuthenticationException {
		UsernamePasswordToken upat = (UsernamePasswordToken) token;
		User user = userRepository.findByUsername(upat.getUsername());
		if (user != null) {
			return new SimpleAuthenticationInfo(user, new BCryptPasswordHash(user.getPasswordHash(), securityConfig), getName());
		} else {
			//TODO don't let the user know that we know that he did not exist
			throw new IncorrectCredentialsException("Invalid username!");
		}
	}

}
