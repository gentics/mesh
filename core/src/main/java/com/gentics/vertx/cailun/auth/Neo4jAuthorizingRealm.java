package com.gentics.vertx.cailun.auth;

import io.vertx.ext.graph.neo4j.Neo4jGraphVerticle;

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
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Transaction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import com.gentics.vertx.cailun.perm.GroupRepository;
import com.gentics.vertx.cailun.perm.RoleRepository;
import com.gentics.vertx.cailun.perm.UserRepository;
import com.gentics.vertx.cailun.perm.model.Group;
import com.gentics.vertx.cailun.perm.model.PermissionSet;
import com.gentics.vertx.cailun.perm.model.Role;
import com.gentics.vertx.cailun.perm.model.User;

public class Neo4jAuthorizingRealm extends AuthorizingRealm {

	private static final Logger log = LoggerFactory.getLogger(Neo4jAuthorizingRealm.class);

	@Autowired
	SecurityConfiguration securityConfig;

	@Autowired
	UserRepository userRepository;

	@Autowired
	GroupRepository groupRepository;

	@Autowired
	RoleRepository roleRepository;

	@Override
	protected AuthorizationInfo doGetAuthorizationInfo(PrincipalCollection principals) {

		Set<String> roles = new HashSet<>();
		Set<Permission> permissions = new HashSet<>();
		GraphDatabaseService graphDb;
		try {
			graphDb = Neo4jGraphVerticle.getDatabase();
			try (Transaction tx = graphDb.beginTx()) {
				// The principal does only hold a string identifier that can be used to load the user pojo from the database.
				// I assume this also has a positive aspect since the session can easily be shared between vertx instances.
				// TODO explicit type check
				User user = userRepository.findByPrincipalId(principals.getPrimaryPrincipal().toString());

				for (Group group : groupRepository.listAllGroups(user)) {
					for (Role role : group.getRoles()) {
						log.info("Loaded role {" + role.getName() + "} to fetch permissionsets..");
						roles.add(role.getName());
						for (PermissionSet permSet : role.getPermissions()) {
							log.info("Loaded permission set for object {" + permSet.getObject().getName() + "}");
							permissions.addAll(permSet.getAllSetPermissions());
						}
					}
				}
			}
		} catch (Exception e) {
			log.error("Could not fetch permission data from neo4j database.", e);
		}

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
			// TODO don't let the user know that we know that he did not exist
			throw new IncorrectCredentialsException("Invalid username!");
		}
	}

}
