package com.gentics.mesh.core.data.service;

import static com.gentics.mesh.core.data.model.relationship.Permission.CREATE_PERM;
import static com.gentics.mesh.core.data.model.relationship.Permission.DELETE_PERM;
import static com.gentics.mesh.core.data.model.relationship.Permission.READ_PERM;
import static com.gentics.mesh.core.data.model.relationship.Permission.UPDATE_PERM;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.annotation.PostConstruct;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.gentics.mesh.core.Page;
import com.gentics.mesh.core.data.model.generic.GenericNode;
import com.gentics.mesh.core.data.model.generic.MeshVertex;
import com.gentics.mesh.core.data.model.relationship.Permission;
import com.gentics.mesh.core.data.model.root.RoleRoot;
import com.gentics.mesh.core.data.model.tinkerpop.MeshAuthUser;
import com.gentics.mesh.core.data.model.tinkerpop.Role;
import com.gentics.mesh.paging.PagingInfo;
import com.gentics.mesh.util.InvalidArgumentException;
import com.gentics.mesh.util.TraversalHelper;
import com.syncleus.ferma.traversals.VertexTraversal;
import com.tinkerpop.blueprints.Vertex;

@Component
public class RoleService extends AbstractMeshService {

	public static RoleService instance;

	@PostConstruct
	public void setup() {
		instance = this;
	}

	public static RoleService getRoleService() {
		return instance;
	}

	@Autowired
	private MeshUserService userService;

	public Role findByUUID(String uuid) {
		return fg.v().has("uuid", uuid).nextOrDefault(Role.class, null);
	}

	public Role findByName(String name) {
		return fg.v().has("name", name).nextOrDefault(Role.class, null);
	}

	public List<? extends Role> findAll() {

		// public Page<Role> findAll(String userUuid, Pageable pageable) {
		// // @Query(value = MATCH_PERMISSION_ON_ROLE + " WHERE " + FILTER_USER_PERM + "return role ORDER BY role.name",
		//
		// // countQuery = MATCH_PERMISSION_ON_ROLE + " WHERE " + FILTER_USER_PERM + " return count(role)")
		// return null;
		// }
		// TODO filter for permissions?
		return fg.v().has(Role.class).toListExplicit(Role.class);
	}

	public void addCRUDPermissionOnRole(MeshAuthUser requestUser, MeshVertex node, Permission permission, GenericNode targetNode) {

		// 1. Determine all roles that grant given permission
		// Node userNode = neo4jTemplate.getPersistentState(user);
		Vertex userNode = requestUser.getVertex();
		Set<Role> roles = new HashSet<>();

		// TODO use core blueprint api or gremlin traversal?
		// for (Edge rel : graphDb.traversalDescription().depthFirst().relationships(AuthRelationships.TYPES.MEMBER_OF, Direction.OUT)
		// .relationships(AuthRelationships.TYPES.HAS_ROLE, Direction.IN)
		// .relationships(AuthRelationships.TYPES.HAS_PERMISSION, Direction.OUT).uniqueness(Uniqueness.RELATIONSHIP_GLOBAL)
		// .traverse(userNode).relationships()) {
		//
		// if (AuthRelationships.HAS_PERMISSION.equalsIgnoreCase(rel.getLabel())) {
		// // Check whether this relation in fact targets our object we want to check
		// boolean matchesTargetNode = rel.getVertex(com.tinkerpop.blueprints.Direction.OUT).getId() == meshPermission.getTargetNode().getId();
		// if (matchesTargetNode) {
		// // Convert the api relationship to a framed edge
		// GraphPermission perm = framedGraph.frame(rel, GraphPermission.class);
		// if (meshPermission.implies(perm) == true) {
		// // This permission is permitting. Add it to the list of roles
		// roles.add(perm.getRole());
		// }
		// }
		// }
		// }

		// 2. Add CRUD permission to identified roles and target node
		for (Role role : roles) {
			role.addPermissions(targetNode, CREATE_PERM, READ_PERM, UPDATE_PERM, DELETE_PERM);
		}
	}

	public Page<? extends Role> findAll(MeshAuthUser requestUser, PagingInfo pagingInfo) throws InvalidArgumentException {
		// TODO filter for permissions
		VertexTraversal traversal = fg.v().has(Role.class);
		VertexTraversal countTraversal = fg.v().has(Role.class);
		return TraversalHelper.getPagedResult(traversal, countTraversal, pagingInfo, Role.class);
	}

	// public GraphPermission findPermission(Long roleId, Long nodeId) {
	// // @Query("MATCH (role:Role)-[r:HAS_PERMISSION]->(node) WHERE id(node) = {1} AND id(role) = {0} return r")
	// return null;
	// }

	public RoleRoot findRoot() {
		// TODO use static reference of mesh root and edge instead?
		return fg.v().has(RoleRoot.class).nextOrDefault(RoleRoot.class, null);
	}

	public Role create(String name) {
		Role role = fg.addFramedVertex(Role.class);
		role.setName(name);
		RoleRoot root = findRoot();
		root.addRole(role);
		return role;
	}

	public RoleRoot createRoot() {
		RoleRoot root = fg.addFramedVertex(RoleRoot.class);
		return root;
	}

	public Role findOne(Object id) {
		Vertex vertex = fg.getVertex(id);
		if (vertex != null) {
			return fg.frameElement(vertex, Role.class);
		}
		return null;
	}

}
