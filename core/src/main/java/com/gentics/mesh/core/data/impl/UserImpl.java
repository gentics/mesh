package com.gentics.mesh.core.data.impl;

import static com.gentics.mesh.core.data.relationship.MeshRelationships.HAS_GROUP;
import static com.gentics.mesh.core.data.relationship.MeshRelationships.HAS_ROLE;
import static com.gentics.mesh.core.data.relationship.MeshRelationships.HAS_USER;
import static com.gentics.mesh.core.data.relationship.Permission.CREATE_PERM;
import static com.gentics.mesh.core.data.relationship.Permission.DELETE_PERM;
import static com.gentics.mesh.core.data.relationship.Permission.READ_PERM;
import static com.gentics.mesh.core.data.relationship.Permission.UPDATE_PERM;
import static com.gentics.mesh.etc.MeshSpringConfiguration.getMeshSpringConfiguration;

import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import org.apache.commons.lang3.BooleanUtils;
import org.springframework.beans.factory.annotation.Configurable;

import com.gentics.mesh.core.data.GenericNode;
import com.gentics.mesh.core.data.Group;
import com.gentics.mesh.core.data.MeshVertex;
import com.gentics.mesh.core.data.Role;
import com.gentics.mesh.core.data.User;
import com.gentics.mesh.core.data.generic.AbstractGenericNode;
import com.gentics.mesh.core.data.relationship.MeshRelationships;
import com.gentics.mesh.core.data.relationship.Permission;
import com.gentics.mesh.core.rest.user.UserResponse;

@Configurable
public class UserImpl extends AbstractGenericNode implements User {

	public static final String FIRSTNAME_KEY = "firstname";

	public static final String LASTNAME_KEY = "lastname";

	public static final String USERNAME_KEY = "username";

	public static final String EMAIL_KEY = "emailAddress";

	public static final String PASSWORD_HASH_KEY = "passwordHash";

	public static final String ENABLED_FLAG = "enabledFlag";

	@Override
	public void disable() {
		setProperty(ENABLED_FLAG, false);
	}

	@Override
	public void deactivate() {
		outE(HAS_GROUP).removeAll();
		disable();
	}

	@Override
	public void enable() {
		setProperty(ENABLED_FLAG, true);
	}

	@Override
	public boolean isEnabled() {
		return BooleanUtils.toBoolean(getProperty(ENABLED_FLAG));
	}

	@Override
	public String getFirstname() {
		return getProperty(FIRSTNAME_KEY);
	}

	@Override
	public void setFirstname(String name) {
		setProperty(FIRSTNAME_KEY, name);
	}

	@Override
	public String getLastname() {
		return getProperty(LASTNAME_KEY);
	}

	@Override
	public void setLastname(String name) {
		setProperty(LASTNAME_KEY, name);
	}

	// TODO add unique index
	@Override
	public String getUsername() {
		return getProperty(USERNAME_KEY);
	}

	@Override
	public void setUsername(String name) {
		setProperty(USERNAME_KEY, name);
	}

	@Override
	public String getEmailAddress() {
		return getProperty(EMAIL_KEY);
	}

	@Override
	public void setEmailAddress(String emailAddress) {
		setProperty(EMAIL_KEY, emailAddress);
	}

	/**
	 * Return all assigned groups.
	 */
	@Override
	public List<? extends Group> getGroups() {
		return out(HAS_USER).has(GroupImpl.class).toListExplicit(GroupImpl.class);

		// public List<? extends Group> listAllGroups(User user) {
		// // @Query("start u=node({0}) MATCH (u)-[MEMBER_OF*]->(g) return g")
		// //
		// return user.getGroups()
		// return framedGraph.v().has(Group.class).mark().in(MEMBER_OF).has(User.class).has("uuid", user.getUuid()).back().toList(Group.class);
		//
		// }
	}

	@Override
	public List<? extends Role> getRoles() {
		return out(HAS_GROUP).out(HAS_ROLE).has(RoleImpl.class).toListExplicit(RoleImpl.class);
	}

	//
	// public Set<GraphPermission> findGraphPermissions(MeshVertex node) {
	//
	// // Set<GraphPermission> permissions = new HashSet<>();
	// // Vertex userNode = user.getVertex();
	// // Node userNode = neo4jTemplate.getPersistentState(user);
	//
	// // Traverse the graph from user to the page. Collect all permission relations and check them individually
	// // for (Edge edge : graphDb.traversalDescription().depthFirst().relationships(AuthRelationships.MEMBER_OF, Direction.OUT)
	// // .relationships(AuthRelationships.HAS_ROLE, Direction.IN).relationships(AuthRelationships.HAS_PERMISSION, Direction.OUT)
	// // .uniqueness(Uniqueness.RELATIONSHIP_GLOBAL).traverse(userNode).relationships()) {
	// // // log.info("Found Relationship " + rel.getType().name() + " between: " + rel.getEndNode().getId() + rel.getEndNode().getLabels() + " and "
	// // // + rel.getStartNode().getId() + rel.getStartNode().getLabels());
	// //
	// // if (AuthRelationships.HAS_PERMISSION.equalsIgnoreCase(edge.getLabel())) {
	// // // Check whether this relation in fact targets our object we want to check
	// // boolean matchesTargetNode = edge.getVertex(com.tinkerpop.blueprints.Direction.OUT).getId() == node.getId();
	// // if (matchesTargetNode) {
	// // // Convert the api relationship to a SDN relationship
	// // GraphPermission perm = framedGraph.frame(edge, GraphPermission.class);
	// // permissions.add(perm);
	// // }
	// // }
	// // }
	// return permissions;
	//
	// }

	@Override
	public String[] getPermissionNames(MeshVertex node) {
		Set<Permission> permissions = getPermissions(node);
		String[] strings = new String[permissions.size()];
		Iterator<Permission> it = permissions.iterator();
		for (int i = 0; i < permissions.size(); i++) {
			strings[i] = it.next().name();
		}
		return strings;
	}

	@Override
	public Set<Permission> getPermissions(MeshVertex node) {

		Set<Permission> permissions = new HashSet<>();
		Set<? extends String> labels = out(HAS_USER).in(HAS_ROLE).outE(Permission.labels()).mark().inV().retain(node.getImpl()).back().label()
				.toSet();
		for (String label : labels) {
			permissions.add(Permission.valueOfLabel(label));
		}
		return permissions;
	}

	@Override
	public boolean hasPermission(MeshVertex node, Permission permission) {
		// TraversalHelper.debug(out(HAS_USER).in(HAS_ROLE).outE(permission.label()).inV());
		// System.out.println(out(HAS_USER).in(HAS_ROLE).outE(permission.label()).mark().inV().retain(node).back().next().getLabel());
		// System.out.println("-----");
		// try {
		return out(HAS_USER).in(HAS_ROLE).outE(permission.label()).mark().inV().retain(node.getImpl()).back().hasNext();
		// } catch (Exception e) {
		// e.printStackTrace();
		// }
		// return false;
	}

	@Override
	public UserResponse transformToRest() {
		UserResponse restUser = new UserResponse();
		restUser.setUuid(getUuid());
		restUser.setUsername(getUsername());
		restUser.setEmailAddress(getEmailAddress());
		restUser.setFirstname(getFirstname());
		restUser.setLastname(getLastname());

		for (Group group : getGroups()) {
			restUser.addGroup(group.getName());
		}
		return restUser;
	}

	@Override
	public void addGroup(Group group) {
		linkOut(group.getImpl(), HAS_USER);
	}

	@Override
	public long getGroupCount() {
		return out(HAS_USER).has(GroupImpl.class).count();
	}

	@Override
	public String getPasswordHash() {
		return getProperty(PASSWORD_HASH_KEY);
	}

	@Override
	public void setPasswordHash(String hash) {
		setProperty(PASSWORD_HASH_KEY, hash);
	}

	@Override
	public void addCRUDPermissionOnRole(MeshVertex node, Permission permission, GenericNode targetNode) {

		// 1. Determine all roles that grant given permission
		List<? extends Role> rolesThatGrantPermission = node.getImpl().in(permission.label()).has(RoleImpl.class).toListExplicit(RoleImpl.class);

		// 2. Add CRUD permission to identified roles and target node
		for (Role role : rolesThatGrantPermission) {
			role.addPermissions(targetNode, CREATE_PERM, READ_PERM, UPDATE_PERM, DELETE_PERM);
		}
	}

	// public boolean isPermitted(long userNodeId, MeshPermission genericPermission) throws Exception {
	// if (genericPermission.getTargetNode() == null) {
	// return false;
	// }
	// Vertex userNode = framedGraph.getVertex(userNodeId);
	// for (Edge groupRel : userNode.getEdges(com.tinkerpop.blueprints.Direction.OUT, MEMBER_OF)) {
	// Vertex group = groupRel.getVertex(com.tinkerpop.blueprints.Direction.OUT);
	// log.debug("Found group: " + group.getProperty("name"));
	// for (Edge roleRel : group.getEdges(Direction.IN, HAS_ROLE)) {
	// Vertex role = roleRel.getVertex(Direction.IN);
	// log.debug("Found role: " + role.getProperty("name"));
	// for (Edge authRel : role.getEdges(Direction.OUT, HAS_PERMISSION)) {
	// log.debug("Permission from {" + authRel.getVertex(Direction.IN).getId() + " to " + authRel.getVertex(Direction.OUT).getId());
	// boolean matchesTargetNode = authRel.getVertex(Direction.OUT).getId() == genericPermission.getTargetNode().getId();
	// if (matchesTargetNode) {
	// log.debug("Found permission");
	// // Convert the api relationship to a framed edge
	// GraphPermission perm = framedGraph.frameElement(authRel, GraphPermission.class);
	// if (genericPermission.implies(perm) == true) {
	// return true;
	// }
	// }
	// }
	// }
	// }

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
	// return false;
	// }

	@Override
	public void delete() {
		// TODO we should not really delete users. Instead we should remove those from all groups and deactivate the access.
		outE(HAS_USER).removeAll();
		disable();
	}

	/**
	 * Encode the given password and set the generated hash.
	 * 
	 * @param password
	 */
	@Override
	public void setPassword(String password) {
		setPasswordHash(getMeshSpringConfiguration().passwordEncoder().encode(password));
	}

	@Override
	public UserImpl getImpl() {
		return this;
	}

}
