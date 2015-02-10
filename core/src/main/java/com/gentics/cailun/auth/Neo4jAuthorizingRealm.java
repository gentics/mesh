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
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.neo4j.support.Neo4jTemplate;

import com.gentics.cailun.core.repository.UserRepository;
import com.gentics.cailun.core.rest.model.auth.AuthRelationships;
import com.gentics.cailun.core.rest.model.auth.BasicPermission;
import com.gentics.cailun.core.rest.model.auth.GraphPermission;
import com.gentics.cailun.core.rest.model.auth.User;
import com.gentics.cailun.etc.CaiLunSpringConfiguration;

public class Neo4jAuthorizingRealm extends AuthorizingRealm {

	@Autowired
	CaiLunSpringConfiguration securityConfig;

	@Autowired
	Neo4jTemplate template;

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

	private boolean checkPermission(long userNodeId, BasicPermission genericPermission) throws Exception {
		if (genericPermission.getTargetNode() == null) {
			return false;
		}
		GraphDatabaseService graphDb = Neo4jGraphVerticle.getDatabase();
		try (Transaction tx = graphDb.beginTx()) {
			Node userNode = graphDb.getNodeById(userNodeId);
			// Traverse the graph from user to the page. Collect all permission relations and check them individually
			for (Relationship rel : graphDb.traversalDescription().depthFirst().relationships(AuthRelationships.TYPES.MEMBER_OF, Direction.OUTGOING)
					.relationships(AuthRelationships.TYPES.HAS_ROLE, Direction.INCOMING)
					.relationships(AuthRelationships.TYPES.HAS_PERMISSION, Direction.OUTGOING).uniqueness(Uniqueness.RELATIONSHIP_GLOBAL)
					.traverse(userNode).relationships()) {

				if (AuthRelationships.HAS_PERMISSION.equalsIgnoreCase(rel.getType().name())) {
					// Check whether this relation in fact targets our object we want to check
//					log.debug("REL: " + rel.getEndNode().getId() + " " + rel.getEndNode().getLabels() + " " + rel.getStartNode().getId()
//							+ " " + rel.getStartNode().getLabels());
					boolean matchesTargetNode = rel.getEndNode().getId() == genericPermission.getTargetNode().getId();
					if (matchesTargetNode) {
						// Convert the api relationship to a SDN relationship
						GraphPermission perm = template.load(rel, GraphPermission.class);
						if (genericPermission.implies(perm) == true) {
							return true;
						}
					}
				}
			}
		}
		return false;
	}

	public boolean isPermitted(PrincipalCollection principals, Permission permission) {
		if (permission instanceof BasicPermission) {
			BasicPermission basicPermission = (BasicPermission) permission;
			try {
				long userId = getNodeIdFromPrincipalId(principals.getPrimaryPrincipal().toString());
				return checkPermission(userId, basicPermission);
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
