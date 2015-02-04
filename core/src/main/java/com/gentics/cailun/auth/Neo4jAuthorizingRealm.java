package com.gentics.cailun.auth;

import io.vertx.ext.graph.neo4j.Neo4jGraphVerticle;

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

import com.gentics.cailun.core.repository.UserRepository;
import com.gentics.cailun.core.rest.model.auth.AbstractShiroGraphPermission;
import com.gentics.cailun.core.rest.model.auth.AuthRelationships;
import com.gentics.cailun.core.rest.model.auth.User;
import com.gentics.cailun.etc.CaiLunSpringConfiguration;

public class Neo4jAuthorizingRealm extends AuthorizingRealm {

	private static final Logger log = LoggerFactory.getLogger(Neo4jAuthorizingRealm.class);

	@Autowired
	CaiLunSpringConfiguration securityConfig;

	@Autowired
	UserRepository userRepository;

	@Override
	protected AuthorizationInfo doGetAuthorizationInfo(PrincipalCollection principals) {
		return new SimpleAuthorizationInfo();
	}

	private long getNodeIdFromPrincipalId(String id) {
		String nodeIdStr = id.substring(id.lastIndexOf("#") + 1);
		return Long.valueOf(nodeIdStr);
	}

	private boolean checkPermission(long userNodeId, AbstractShiroGraphPermission genericPermission) throws Exception {
		if (genericPermission.getTargetNode() == null) {
			return false;
		}
		GraphDatabaseService graphDb = Neo4jGraphVerticle.getDatabase();
		try (Transaction tx = graphDb.beginTx()) {
			// Neo4jTemplate template = new Neo4jTemplate(graphDb);
			// template.getPersistentState(entity)
			Node userNode = graphDb.getNodeById(userNodeId);
			// Traverse the graph from user to the page. Collect all permission relations and check them individually
			for (Relationship rel : graphDb.traversalDescription().depthFirst().relationships(AuthRelationships.TYPES.MEMBER_OF, Direction.OUTGOING)
					.relationships(AuthRelationships.TYPES.HAS_ROLE, Direction.INCOMING).relationships(AuthRelationships.TYPES.HAS_PERMISSION)
					.relationships(AuthRelationships.TYPES.ASSIGNED_TO).uniqueness(Uniqueness.RELATIONSHIP_GLOBAL).traverse(userNode).relationships()) {

				// Check whether this relation in fact targets our object we want to check
				boolean matchesTargetNode = rel.getStartNode().getId() == genericPermission.getTargetNode().getId();
				if (matchesTargetNode) {
					// Check whether the label of the permission node matches the label of the shiro permission
					boolean matchesPermissionLabel = rel.getEndNode().hasLabel(genericPermission.getPermissionNodeLabel());
					if (matchesPermissionLabel) {
						// Delegate the final permission check to the shiro permission
						if (genericPermission.isPermitted(rel.getEndNode(), rel.getStartNode()) == true) {
							return true;
						}
					}
				}

			}
		}
		return false;
	}

	public boolean isPermitted(PrincipalCollection principals, Permission permission) {
		if (permission instanceof AbstractShiroGraphPermission) {
			AbstractShiroGraphPermission genericPermission = (AbstractShiroGraphPermission) permission;
			try {
				long userId = getNodeIdFromPrincipalId(principals.getPrimaryPrincipal().toString());
				return checkPermission(userId, genericPermission);
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
