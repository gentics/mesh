package com.gentics.cailun.auth;

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
import org.neo4j.graphdb.Direction;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Relationship;
import org.neo4j.graphdb.Transaction;
import org.neo4j.graphdb.traversal.Uniqueness;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import com.gentics.cailun.core.repository.GroupRepository;
import com.gentics.cailun.core.repository.RoleRepository;
import com.gentics.cailun.core.repository.UserRepository;
import com.gentics.cailun.core.rest.model.AuthRelationships;
import com.gentics.cailun.core.rest.model.GenericPermission;
import com.gentics.cailun.core.rest.model.User;
import com.gentics.cailun.etc.CaiLunSpringConfiguration;
import com.gentics.cailun.etc.Neo4jSpringConfiguration;

public class Neo4jAuthorizingRealm extends AuthorizingRealm {

	private static final Logger log = LoggerFactory.getLogger(Neo4jAuthorizingRealm.class);

	@Autowired
	CaiLunSpringConfiguration securityConfig;

	@Autowired
	UserRepository userRepository;

	@Autowired
	Neo4jSpringConfiguration neo4jConfig;

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
				// User user = userRepository.findByPrincipalId(principals.getPrimaryPrincipal().toString());

				// for (Group group : groupRepository.listAllGroups(user)) {
				// for (Role role : group.getRoles()) {
				// log.debug("Loaded role {" + role.getName() + "} to fetch permissionsets..");
				// roles.add(role.getName());
				// for (PermissionSet permSet : role.getPermissions()) {
				// log.debug("Loaded permission set for object {" + permSet.getObject().getName() + "}");
				// permissions.addAll(permSet.getAllSetPermissions());
				// }
				// }
				// }
			}
		} catch (Exception e) {
			log.error("Could not fetch permission data from neo4j database.", e);
		}

		SimpleAuthorizationInfo info = new SimpleAuthorizationInfo(roles);
		info.setRoles(roles);
		info.setObjectPermissions(permissions);
		return info;
	}

	private long getNodeIdFromPrincipalId(String id) {
		String nodeIdStr = id.substring(id.lastIndexOf("#") + 1);
		return Long.valueOf(nodeIdStr);
	}

	private boolean canRead(long userNodeId, long targetNodeId) throws Exception {
		GraphDatabaseService graphDb = Neo4jGraphVerticle.getDatabase();
		try (Transaction tx = graphDb.beginTx()) {
			Node userNode = graphDb.getNodeById(userNodeId);
			// Traverse the graph from user to the page. Collect all permission relations and check them individually
			for (Relationship rel : graphDb.traversalDescription().depthFirst().relationships(AuthRelationships.MEMBER_OF, Direction.OUTGOING)
					.relationships(AuthRelationships.HAS_ROLE, Direction.INCOMING)
					.relationships(AuthRelationships.HAS_PERMISSIONSET, Direction.OUTGOING).uniqueness(Uniqueness.RELATIONSHIP_GLOBAL)
					.traverse(userNode).relationships()) {

				// Check whether the current relation is in fact a permission set relation 
				if (AuthRelationships.HAS_PERMISSIONSET.name().equalsIgnoreCase(rel.getType().name())) {
					// Check whether this relation is targeting the object we want to check
					if (rel.getEndNode().getId() == targetNodeId) {
						// Finally check whether this relation has in fact the needed permission
						if ((boolean) rel.getProperty("canRead") == true) {
							return true;
						}
					}
					
				}
			}
		}
		return false;
	}

	public boolean isPermitted(PrincipalCollection principals, Permission permission) {
		if (permission instanceof GenericPermission) {
			GenericPermission genericPermission = (GenericPermission) permission;
			try {
				return canRead(getNodeIdFromPrincipalId(principals.getPrimaryPrincipal().toString()), genericPermission.getTargetObject().getId());
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		return false;
	}

	@Override
	protected AuthenticationInfo doGetAuthenticationInfo(AuthenticationToken token) throws AuthenticationException {
		UsernamePasswordToken upat = (UsernamePasswordToken) token;
		User user = userRepository.findByUsername(upat.getUsername());
		if (user != null) {
			return new SimpleAuthenticationInfo(user, new BCryptPasswordHash(user.getPasswordHash(), securityConfig), getName());
		} else {
			// TODO Don't let the user know that we know that he did not exist
			throw new IncorrectCredentialsException("Invalid credentials!");
		}
	}
}
