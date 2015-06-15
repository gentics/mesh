package com.gentics.mesh.core.data.service;

import io.vertx.core.logging.Logger;
import io.vertx.core.logging.impl.LoggerFactory;
import io.vertx.ext.apex.RoutingContext;
import io.vertx.ext.apex.Session;

import java.awt.print.Pageable;
import java.util.HashSet;
import java.util.Set;

import org.springframework.stereotype.Component;

import com.gentics.mesh.core.Page;
import com.gentics.mesh.core.data.model.auth.AuthRelationships;
import com.gentics.mesh.core.data.model.auth.MeshPermission;
import com.gentics.mesh.core.data.model.auth.PermissionType;
import com.gentics.mesh.core.data.model.generic.MeshVertex;
import com.gentics.mesh.core.data.model.root.UserRoot;
import com.gentics.mesh.core.data.model.tinkerpop.GraphPermission;
import com.gentics.mesh.core.data.model.tinkerpop.Group;
import com.gentics.mesh.core.data.model.tinkerpop.User;
import com.gentics.mesh.core.rest.user.response.UserResponse;
import com.gentics.mesh.error.HttpStatusCodeErrorException;
import com.gentics.mesh.paging.PagingInfo;
import com.tinkerpop.blueprints.Direction;
import com.tinkerpop.blueprints.Edge;
import com.tinkerpop.blueprints.Vertex;

@Component
public class UserService extends AbstractMeshService {

	private static final Logger log = LoggerFactory.getLogger(UserService.class);

	//	@Autowired
	//	protected ExposingFramedGraph framedGraph;

	public void setPassword(User user, String password) {
		user.setPasswordHash(springConfiguration.passwordEncoder().encode(password));
	}

	public Page<User> findAllVisible(RoutingContext rc, PagingInfo pagingInfo) {
		Session session = rc.session();
		// String userUuid = session.getPrincipal().getString("uuid");
		// return findAll(userUuid, new MeshPageRequest(pagingInfo));
		return null;
	}

	public User findByUsername(String username) {
		return framedGraph.v().has("username", username).has("ferma_type", User.class.getName()).next(User.class);
	}

	public UserResponse transformToRest(User user) {
		if (user == null) {
			throw new HttpStatusCodeErrorException(500, "User can't be null");
		}
		UserResponse restUser = new UserResponse();
		restUser.setUuid(user.getUuid());
		restUser.setUsername(user.getUsername());
		restUser.setEmailAddress(user.getEmailAddress());
		restUser.setFirstname(user.getFirstname());
		restUser.setLastname(user.getLastname());

		for (Group group : user.getGroups()) {
			restUser.addGroup(group.getName());
		}
		return restUser;
	}

	public void removeUserFromGroup(User user, Group group) {
		group.removeUser(user);
	}

	public Set<GraphPermission> findGraphPermissions(User user, MeshVertex node) {

		Set<GraphPermission> permissions = new HashSet<>();
		Vertex userNode = user.getVertex();
		// Node userNode = neo4jTemplate.getPersistentState(user);

		// Traverse the graph from user to the page. Collect all permission relations and check them individually
		// for (Edge edge : graphDb.traversalDescription().depthFirst().relationships(AuthRelationships.MEMBER_OF, Direction.OUT)
		// .relationships(AuthRelationships.HAS_ROLE, Direction.IN).relationships(AuthRelationships.HAS_PERMISSION, Direction.OUT)
		// .uniqueness(Uniqueness.RELATIONSHIP_GLOBAL).traverse(userNode).relationships()) {
		// // log.info("Found Relationship " + rel.getType().name() + " between: " + rel.getEndNode().getId() + rel.getEndNode().getLabels() + " and "
		// // + rel.getStartNode().getId() + rel.getStartNode().getLabels());
		//
		// if (AuthRelationships.HAS_PERMISSION.equalsIgnoreCase(edge.getLabel())) {
		// // Check whether this relation in fact targets our object we want to check
		// boolean matchesTargetNode = edge.getVertex(com.tinkerpop.blueprints.Direction.OUT).getId() == node.getId();
		// if (matchesTargetNode) {
		// // Convert the api relationship to a SDN relationship
		// GraphPermission perm = framedGraph.frame(edge, GraphPermission.class);
		// permissions.add(perm);
		// }
		// }
		// }
		return permissions;

	}

	public boolean isPermitted(long userNodeId, MeshPermission genericPermission) throws Exception {
		if (genericPermission.getTargetNode() == null) {
			return false;
		}
		Vertex userNode = framedGraph.getVertex(userNodeId);
		for (Edge groupRel : userNode.getEdges(com.tinkerpop.blueprints.Direction.OUT, AuthRelationships.MEMBER_OF)) {
			Vertex group = groupRel.getVertex(com.tinkerpop.blueprints.Direction.OUT);
			log.debug("Found group: " + group.getProperty("name"));
			for (Edge roleRel : group.getEdges(Direction.IN, AuthRelationships.HAS_ROLE)) {
				Vertex role = roleRel.getVertex(Direction.IN);
				log.debug("Found role: " + role.getProperty("name"));
				for (Edge authRel : role.getEdges(Direction.OUT, AuthRelationships.HAS_PERMISSION)) {
					log.debug("Permission from {" + authRel.getVertex(Direction.IN).getId() + " to " + authRel.getVertex(Direction.OUT).getId());
					boolean matchesTargetNode = authRel.getVertex(Direction.OUT).getId() == genericPermission.getTargetNode().getId();
					if (matchesTargetNode) {
						log.debug("Found permission");
						// Convert the api relationship to a framed edge
						GraphPermission perm = framedGraph.frameElement(authRel, GraphPermission.class);
						if (genericPermission.implies(perm) == true) {
							return true;
						}
					}
				}
			}
		}

		// // Traverse the graph from user to the page. Collect all permission relations and check them individually
		// for (Relationship rel : graphDb.traversalDescription().depthFirst().relationships(AuthRelationships.TYPES.MEMBER_OF, Direction.OUTGOING)
		// .relationships(AuthRelationships.TYPES.HAS_ROLE, Direction.INCOMING)
		// .relationships(AuthRelationships.TYPES.HAS_PERMISSION, Direction.OUTGOING).uniqueness(Uniqueness.RELATIONSHIP_GLOBAL)
		// .traverse(userNode).relationships()) {
		// // log.debug("Found Relationship " + rel.getType().name() + " between: " + rel.getEndNode().getId() + rel.getEndNode().getLabels() + " and "
		// // + rel.getStartNode().getId() + rel.getStartNode().getLabels());
		//
		// if (AuthRelationships.HAS_PERMISSION.equalsIgnoreCase(rel.getType().name())) {
		// // Check whether this relation in fact targets our object we want to check
		// boolean matchesTargetNode = rel.getEndNode().getId() == genericPermission.getTargetNode().getId();
		// if (matchesTargetNode) {
		// // Convert the api relationship to a SDN relationship
		// GraphPermission perm = neo4jTemplate.load(rel, GraphPermission.class);
		// if (genericPermission.implies(perm) == true) {
		// return true;
		// }
		// }
		// }
		// }
		return false;
	}

	public String[] getPerms(RoutingContext rc, MeshVertex node) {
		String uuid = rc.session().getPrincipal().getString("uuid");
		User user = findByUUID(uuid);
		Set<GraphPermission> permissions = findGraphPermissions(user, node);
		Set<String> perms = new HashSet<>();
		for (GraphPermission perm : permissions) {
			if (perm.isPermitted(PermissionType.READ)) {
				perms.add(PermissionType.READ.getPropertyName());
			}
			if (perm.isPermitted(PermissionType.DELETE)) {
				perms.add(PermissionType.DELETE.getPropertyName());
			}
			if (perm.isPermitted(PermissionType.CREATE)) {
				perms.add(PermissionType.CREATE.getPropertyName());
			}
			if (perm.isPermitted(PermissionType.UPDATE)) {
				perms.add(PermissionType.UPDATE.getPropertyName());
			}
		}
		return perms.toArray(new String[perms.size()]);
	}

	public User findUser(RoutingContext rc) {
		String userUuid = rc.session().getPrincipal().getString("uuid");
		return findByUUID(userUuid);
	}

	public Page<User> findByGroup(RoutingContext rc, Group group, PagingInfo pagingInfo) {
		String userUuid = rc.session().getPrincipal().getString("uuid");

		// @Query(value =
		// "MATCH (requestUser:User)-[:MEMBER_OF]->(group:Group)<-[:HAS_ROLE]-(role:Role)-[perm:HAS_PERMISSION]->(user:User) MATCH (user)-[:MEMBER_OF]-(group:Group) where id(group) = {1} AND requestUser.uuid = {0} and perm.`permissions-read` = true return user ORDER BY user.username",
		// countQuery =
		// "MATCH (requestUser:User)-[:MEMBER_OF]->(group:Group)<-[:HAS_ROLE]-(role:Role)-[perm:HAS_PERMISSION]->(user:User) MATCH (user)-[:MEMBER_OF]-(group:Group) where id(group) = {1} AND requestUser.uuid = {0} and perm.`permissions-read` = true return count(user)")
		// Page<User> findByGroup(String userUuid, Group group, Pageable pageable);
		// return findByGroup(userUuid, group, new MeshPageRequest(pagingInfo));
		return null;
	}

	User findByPrincipalId(String principalId) {
		// @Query("MATCH (u:_User) WHERE u.username + '%' + u.emailAddress + '%' +  u.passwordHash = {0} return u")
		return null;
	}

	/**
	 * Returns a Page of users meeting the paging restriction provided in the Pageable object.
	 * 
	 * @param requestUser
	 * @param pageable
	 * @return
	 */
	public Page<User> findAll(String userUuid, Pageable pageable) {
		// @Query(value =
		// "MATCH (requestUser:User)-[:MEMBER_OF]->(group:Group)<-[:HAS_ROLE]-(role:Role)-[perm:HAS_PERMISSION]->(user:User) where requestUser.uuid = {0} and perm.`permissions-read` = true return user ORDER BY user.username",
		// countQuery =
		// "MATCH (requestUser:User)-[:MEMBER_OF]->(group:Group)<-[:HAS_ROLE]-(role:Role)-[perm:HAS_PERMISSION]->(user:User) where requestUser.uuid = {0} and perm.`permissions-read` = true return count(user)")
		return null;

	}

	public UserRoot findRoot() {
		return framedGraph.v().has("ferma_type", UserRoot.class.getName()).next(UserRoot.class);
	}

	//	@Override
	//	public User save(User user) {
	// UserRoot root = userRepository.findRoot();
	// if (root == null) {
	// throw new NullPointerException("The user root node could not be found.");
	// }
	// user = neo4jTemplate.save(user);
	// root.getUsers().add(user);
	// neo4jTemplate.save(root);
	// return user;
	//		return null;
	//	}

	public User create(String username) {
		User user = framedGraph.addFramedVertex(User.class);
		user.setUsername(username);
		return user;
	}

	public UserRoot createRoot() {
		UserRoot root = framedGraph.addFramedVertex(UserRoot.class);
		return root;
	}

	public User findOne(Long id) {
		Vertex vertex = framedGraph.getVertex(id);
		if (vertex != null) {
			return framedGraph.frameElement(vertex, User.class);
		}
		return null;
	}

	public User findByUUID(String uuid) {
		return framedGraph.v().has("uuid", uuid).has("ferma_type", User.class.getName()).next(User.class);
	}

	public void delete(User user) {
		user.getVertex().remove();
	}

}
