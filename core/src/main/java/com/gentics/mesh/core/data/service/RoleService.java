package com.gentics.mesh.core.data.service;

import static com.gentics.mesh.core.data.relationship.Permission.CREATE_PERM;
import static com.gentics.mesh.core.data.relationship.Permission.DELETE_PERM;
import static com.gentics.mesh.core.data.relationship.Permission.READ_PERM;
import static com.gentics.mesh.core.data.relationship.Permission.UPDATE_PERM;

import java.util.List;

import javax.annotation.PostConstruct;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.gentics.mesh.core.Page;
import com.gentics.mesh.core.data.GenericNode;
import com.gentics.mesh.core.data.MeshAuthUser;
import com.gentics.mesh.core.data.MeshVertex;
import com.gentics.mesh.core.data.Role;
import com.gentics.mesh.core.data.impl.RoleImpl;
import com.gentics.mesh.core.data.relationship.Permission;
import com.gentics.mesh.paging.PagingInfo;
import com.gentics.mesh.util.InvalidArgumentException;
import com.gentics.mesh.util.TraversalHelper;
import com.syncleus.ferma.traversals.VertexTraversal;
import com.tinkerpop.blueprints.Vertex;

@Component
public class RoleService extends AbstractMeshGraphService<Role> {

	@Autowired
	private MeshUserService userService;

	public static RoleService instance;

	@PostConstruct
	public void setup() {
		instance = this;
	}

	public static RoleService getRoleService() {
		return instance;
	}

	public Role findByName(String name) {
		return findByName(name, RoleImpl.class);
	}

	public List<? extends Role> findAll() {

		// public Page<Role> findAll(String userUuid, Pageable pageable) {
		// // @Query(value = MATCH_PERMISSION_ON_ROLE + " WHERE " + FILTER_USER_PERM + "return role ORDER BY role.name",
		//
		// // countQuery = MATCH_PERMISSION_ON_ROLE + " WHERE " + FILTER_USER_PERM + " return count(role)")
		// return null;
		// }
		// TODO filter for permissions?
		return fg.v().has(RoleImpl.class).toListExplicit(RoleImpl.class);
	}

	// public void addCRUDPermissionOnRole(MeshAuthUser requestUser, GroupRoot groupRoot, Permission createPerm, Group group) {

	public void addCRUDPermissionOnRole(MeshAuthUser requestUser, MeshVertex node, Permission permission, GenericNode targetNode) {

		// 1. Determine all roles that grant given permission
		List<? extends Role> rolesThatGrantPermission = node.getImpl().in(permission.label()).has(RoleImpl.class).toListExplicit(RoleImpl.class);

		// 2. Add CRUD permission to identified roles and target node
		for (Role role : rolesThatGrantPermission) {
			role.addPermissions(targetNode, CREATE_PERM, READ_PERM, UPDATE_PERM, DELETE_PERM);
		}
	}

	public Page<? extends Role> findAll(MeshAuthUser requestUser, PagingInfo pagingInfo) throws InvalidArgumentException {
		// TODO filter for permissions
		VertexTraversal traversal = fg.v().has(RoleImpl.class);
		VertexTraversal countTraversal = fg.v().has(RoleImpl.class);
		return TraversalHelper.getPagedResult(traversal, countTraversal, pagingInfo, RoleImpl.class);
	}

	@Override
	public Role findByUUID(String uuid) {
		return findByUUID(uuid, RoleImpl.class);
	}

}
