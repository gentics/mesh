package com.gentics.mesh.core.data.model.tinkerpop;

import static com.gentics.mesh.core.data.model.relationship.MeshRelationships.HAS_USER;

import java.util.List;

import com.gentics.mesh.core.data.model.generic.GenericNode;
import com.gentics.mesh.core.data.model.generic.MeshVertex;
import com.gentics.mesh.core.data.model.relationship.MeshRelationships;

public class User extends GenericNode {

	public static String FIRSTNAME_KEY = "firstname";

	public static String LASTNAME_KEY = "lastname";

	public static String USERNAME_KEY = "username";

	public static String EMAIL_KEY = "emailAddress";

	public static String PASSWORD_HASH_KEY = "passwordHash";

	public String getFirstname() {
		return getProperty(FIRSTNAME_KEY);
	}

	public void setFirstname(String name) {
		setProperty(FIRSTNAME_KEY, name);
	}

	public String getLastname() {
		return getProperty(LASTNAME_KEY);
	}

	public void setLastname(String name) {
		setProperty(LASTNAME_KEY, name);
	}

	// TODO add unique index
	public String getUsername() {
		return getProperty(USERNAME_KEY);
	}

	public void setUsername(String name) {
		setProperty(USERNAME_KEY, name);
	}

	public String getEmailAddress() {
		return getProperty(EMAIL_KEY);
	}

	public void setEmailAddress(String emailAddress) {
		setProperty(EMAIL_KEY, emailAddress);
	}

	/**
	 * Return all groups that are assigned to the user
	 * 
	 * @param user
	 * @return
	 */
	public List<? extends Group> getGroups() {
		return out(HAS_USER).toList(Group.class);

		//		public List<? extends Group> listAllGroups(User user) {
		//			// @Query("start u=node({0}) MATCH (u)-[MEMBER_OF*]->(g) return g")
		//			//
		//			return user.getGroups()
		//			return framedGraph.v().has(Group.class).mark().in(MEMBER_OF).has(User.class).has("uuid", user.getUuid()).back().toList(Group.class);
		//
		//		}
	}
	
	
//	
//	public Set<GraphPermission> findGraphPermissions(MeshVertex node) {
//
////		Set<GraphPermission> permissions = new HashSet<>();
////		Vertex userNode = user.getVertex();
//		// Node userNode = neo4jTemplate.getPersistentState(user);
//
//		// Traverse the graph from user to the page. Collect all permission relations and check them individually
//		// for (Edge edge : graphDb.traversalDescription().depthFirst().relationships(AuthRelationships.MEMBER_OF, Direction.OUT)
//		// .relationships(AuthRelationships.HAS_ROLE, Direction.IN).relationships(AuthRelationships.HAS_PERMISSION, Direction.OUT)
//		// .uniqueness(Uniqueness.RELATIONSHIP_GLOBAL).traverse(userNode).relationships()) {
//		// // log.info("Found Relationship " + rel.getType().name() + " between: " + rel.getEndNode().getId() + rel.getEndNode().getLabels() + " and "
//		// // + rel.getStartNode().getId() + rel.getStartNode().getLabels());
//		//
//		// if (AuthRelationships.HAS_PERMISSION.equalsIgnoreCase(edge.getLabel())) {
//		// // Check whether this relation in fact targets our object we want to check
//		// boolean matchesTargetNode = edge.getVertex(com.tinkerpop.blueprints.Direction.OUT).getId() == node.getId();
//		// if (matchesTargetNode) {
//		// // Convert the api relationship to a SDN relationship
//		// GraphPermission perm = framedGraph.frame(edge, GraphPermission.class);
//		// permissions.add(perm);
//		// }
//		// }
//		// }
//		return permissions;
//
//	}


//	public String[] getPerms(RoutingContext rc, MeshVertex node) {
//	
//	
//		Set<GraphPermission> permissions = findGraphPermissions(node);
//		Set<String> perms = new HashSet<>();
//		for (GraphPermission perm : permissions) {
//			if (perm.isPermitted(READ)) {
//				perms.add(READ.getPropertyName());
//			}
//			if (perm.isPermitted(DELETE)) {
//				perms.add(DELETE.getPropertyName());
//			}
//			if (perm.isPermitted(CREATE)) {
//				perms.add(CREATE.getPropertyName());
//			}
//			if (perm.isPermitted(UPDATE)) {
//				perms.add(UPDATE.getPropertyName());
//			}
//		}
//		return perms.toArray(new String[perms.size()]);
//	}
	
	public boolean hasPermission(MeshVertex vertex) {
		
	}

	public void addGroup(Group group) {
		linkOut(group, HAS_USER);
	}

	public Long getGroupCount() {
		return out(MeshRelationships.HAS_USER).count();
	}

	public String getPasswordHash() {
		return getProperty(PASSWORD_HASH_KEY);
	}

	public void setPasswordHash(String hash) {
		setProperty(PASSWORD_HASH_KEY, hash);
	}

	public String getPrincipalId() {
		return getUsername() + "%" + getEmailAddress() + "%" + getPasswordHash() + "#" + getId();
	}

}
