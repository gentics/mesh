package com.gentics.mesh.auth;

import io.vertx.core.logging.Logger;
import io.vertx.core.logging.impl.LoggerFactory;

import org.apache.shiro.authc.AuthenticationException;
import org.apache.shiro.authc.AuthenticationInfo;
import org.apache.shiro.authc.AuthenticationToken;
import org.apache.shiro.authc.IncorrectCredentialsException;
import org.apache.shiro.authc.SimpleAuthenticationInfo;
import org.apache.shiro.authc.UsernamePasswordToken;
import org.apache.shiro.authz.AuthorizationInfo;
import org.apache.shiro.authz.SimpleAuthorizationInfo;
import org.apache.shiro.realm.AuthorizingRealm;
import org.apache.shiro.subject.PrincipalCollection;
import org.springframework.beans.factory.annotation.Autowired;

import com.gentics.mesh.core.data.model.tinkerpop.MeshUser;
import com.gentics.mesh.core.data.service.MeshUserService;
import com.gentics.mesh.etc.MeshSpringConfiguration;
import com.syncleus.ferma.FramedGraph;

public class GraphBackedAuthorizingRealm extends AuthorizingRealm {

	private static final Logger log = LoggerFactory.getLogger(GraphBackedAuthorizingRealm.class);

	@Autowired
	private MeshSpringConfiguration securityConfig;

	@Autowired
	private MeshUserService userService;

	@Autowired
	private FramedGraph framedGraph;

	@Override
	protected AuthorizationInfo doGetAuthorizationInfo(PrincipalCollection principals) {
		return new SimpleAuthorizationInfo();
	}

//	public boolean isPermitted(PrincipalCollection principals, Permission permission) {
//		if (principals.getPrimaryPrincipal() instanceof Long) {
//			if (permission instanceof WildcardPermission) {
//				WildcardPermission wildcardPermission = (WildcardPermission) permission;
//				String perm = wildcardPermission.toString();
//				int mid = perm.indexOf("#");
//				String targetId = perm.substring(1, mid);
//				String permName = perm.substring(mid + 1, perm.length() - 1);
//				Long userId = (Long) principals.getPrimaryPrincipal();
//				boolean permitted = false;
//					try {
//						Vertex node = framedGraph.getVertex(Long.valueOf(targetId));
//						GenericNode framedNode = framedGraph.frameElement(node, GenericNode.class);
//						Permission type = Permission.(permName);
//						permitted = userService.isPermitted(userId, new MeshPermission(framedNode, type));
//					} catch (Exception e) {
//						throw new HttpStatusCodeErrorException(500, "Error while checking permission for user {" + userId + "}", e);
//					}
//				return permitted;
//			} else {
//				throw new HttpStatusCodeErrorException(500, "Permission format does not match expected values");
//			}
//		} else {
//			throw new HttpStatusCodeErrorException(500, "Permission format does not match expected values");
//		}
//	}

	@Override
	protected AuthenticationInfo doGetAuthenticationInfo(AuthenticationToken token) throws AuthenticationException {
		UsernamePasswordToken upat = (UsernamePasswordToken) token;
		MeshUser user = userService.findByUsername(upat.getUsername());
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
