package com.gentics.mesh.core.data.service;

import static com.gentics.mesh.core.data.model.relationship.MeshRelationships.HAS_ROLE;
import static com.gentics.mesh.core.data.model.relationship.Permission.CREATE_PERM;
import static com.gentics.mesh.core.data.model.relationship.Permission.DELETE_PERM;
import static com.gentics.mesh.core.data.model.relationship.Permission.READ_PERM;
import static com.gentics.mesh.core.data.model.relationship.Permission.UPDATE_PERM;
import io.vertx.ext.web.RoutingContext;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.gentics.mesh.auth.MeshPermission;
import com.gentics.mesh.core.Page;
import com.gentics.mesh.core.data.model.generic.GenericNode;
import com.gentics.mesh.core.data.model.root.RoleRoot;
import com.gentics.mesh.core.data.model.tinkerpop.Group;
import com.gentics.mesh.core.data.model.tinkerpop.Role;
import com.gentics.mesh.core.data.model.tinkerpop.User;
import com.gentics.mesh.core.rest.group.response.GroupResponse;
import com.gentics.mesh.core.rest.role.response.RoleResponse;
import com.gentics.mesh.error.HttpStatusCodeErrorException;
import com.gentics.mesh.paging.PagingInfo;
import com.gentics.mesh.util.InvalidArgumentException;
import com.gentics.mesh.util.PagingHelper;
import com.syncleus.ferma.traversals.VertexTraversal;
import com.tinkerpop.blueprints.Vertex;

@Component
public class RoleService extends AbstractMeshService {

	@Autowired
	private UserService userService;

	public Role findByUUID(String uuid) {
		return framedGraph.v().has("uuid", uuid).has(Role.class).nextExplicit(Role.class);
	}

	public Role findByName(String name) {
		return framedGraph.v().has("name", name).has(Role.class).nextExplicit(Role.class);
	}

	public List<? extends Role> findAll() {

		//		public Page<Role> findAll(String userUuid, Pageable pageable) {
		//			//		@Query(value = MATCH_PERMISSION_ON_ROLE + " WHERE " + FILTER_USER_PERM + "return role ORDER BY role.name",
		//
		//			//		countQuery = MATCH_PERMISSION_ON_ROLE + " WHERE " + FILTER_USER_PERM + " return count(role)")
		//			return null;
		//		}
		//TODO filter for permissions?
		return framedGraph.v().has(Role.class).toListExplicit(Role.class);
	}

	public RoleResponse transformToRest(Role role) {
		if (role == null) {
			throw new HttpStatusCodeErrorException(500, "Role can't be null");
		}
		RoleResponse restRole = new RoleResponse();
		restRole.setUuid(role.getUuid());
		restRole.setName(role.getName());

		for (Group group : role.getGroups()) {
			GroupResponse restGroup = new GroupResponse();
			restGroup.setName(group.getName());
			restGroup.setUuid(group.getUuid());
			restRole.getGroups().add(restGroup);
		}

		return restRole;
	}

	public void addCRUDPermissionOnRole(RoutingContext rc, MeshPermission meshPermission, GenericNode targetNode) {

		User user = userService.findUser(rc);

		// 1. Determine all roles that grant given permission
		//		Node userNode = neo4jTemplate.getPersistentState(user);
		Vertex userNode = user.getVertex();
		Set<Role> roles = new HashSet<>();

		//TODO use core blueprint api or gremlin traversal?
		//		for (Edge rel : graphDb.traversalDescription().depthFirst().relationships(AuthRelationships.TYPES.MEMBER_OF, Direction.OUT)
		//				.relationships(AuthRelationships.TYPES.HAS_ROLE, Direction.IN)
		//				.relationships(AuthRelationships.TYPES.HAS_PERMISSION, Direction.OUT).uniqueness(Uniqueness.RELATIONSHIP_GLOBAL)
		//				.traverse(userNode).relationships()) {
		//
		//			if (AuthRelationships.HAS_PERMISSION.equalsIgnoreCase(rel.getLabel())) {
		//				// Check whether this relation in fact targets our object we want to check
		//				boolean matchesTargetNode = rel.getVertex(com.tinkerpop.blueprints.Direction.OUT).getId() == meshPermission.getTargetNode().getId();
		//				if (matchesTargetNode) {
		//					// Convert the api relationship to a framed edge
		//					GraphPermission perm = framedGraph.frame(rel, GraphPermission.class);
		//					if (meshPermission.implies(perm) == true) {
		//						// This permission is permitting. Add it to the list of roles
		//						roles.add(perm.getRole());
		//					}
		//				}
		//			}
		//		}

		// 2. Add CRUD permission to identified roles and target node
		for (Role role : roles) {
			role.addPermissions(targetNode, CREATE_PERM, READ_PERM, UPDATE_PERM, DELETE_PERM);
		}
	}

	public Page<Role> findAll(RoutingContext rc, PagingInfo pagingInfo) {
//		String userUuid = rc.session().getPrincipal().getString("uuid");
		//		return findAll(userUuid, new MeshPageRequest(pagingInfo));
		return null;
	}

	public Page<? extends Role> findByGroup(RoutingContext rc, Group group, PagingInfo pagingInfo) throws InvalidArgumentException {
		String userUuid = rc.user().principal().getString("uuid");
		//		return findByGroup(userUuid, group, new MeshPageRequest(pagingInfo));
		//	@Query(value = MATCH_PERMISSION_ON_ROLE + " MATCH (role)-[:HAS_ROLE]->(group:Group) where id(group) = {1} AND " + FILTER_USER_PERM
		//			+ " return role ORDER BY role.name desc",

		//	countQuery = MATCH_PERMISSION_ON_ROLE + "MATCH (role)-[:HAS_ROLE]->(group:Group) where id(group) = {1} AND " + FILTER_USER_PERM
		//			+ "return count(role)")
		//		Page<Role> findByGroup(String userUuid, Group group, Pageable pageable) {
		//			return null;
		//		}
		VertexTraversal<?, ?, ?> traversal = framedGraph.v().has(Role.class).mark().out(HAS_ROLE).hasId(group.getId()).back();

		Page<? extends Role> page = PagingHelper.getPagedResult(traversal, pagingInfo, Role.class);
		return page;

	}

	//	public GraphPermission findPermission(Long roleId, Long nodeId) {
	//		//	@Query("MATCH (role:Role)-[r:HAS_PERMISSION]->(node) WHERE id(node) = {1} AND id(role) = {0} return r")
	//		return null;
	//	}

	public RoleRoot findRoot() {
		return framedGraph.v().has(RoleRoot.class).nextExplicit(RoleRoot.class);
	}

	public Role create(String name) {
		Role role = framedGraph.addFramedVertex(Role.class);
		role.setName(name);
		RoleRoot root = findRoot();
		root.addRole(role);
		return role;
	}

	public RoleRoot createRoot() {
		RoleRoot root = framedGraph.addFramedVertex(RoleRoot.class);
		return root;
	}

	public Role findOne(Long id) {
		Vertex vertex = framedGraph.getVertex(id);
		if (vertex != null) {
			return framedGraph.frameElement(vertex, Role.class);
		}
		return null;
	}

	public void delete(Role role) {
		role.getVertex().remove();
	}

}
