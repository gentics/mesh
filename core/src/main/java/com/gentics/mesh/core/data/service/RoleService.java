package com.gentics.mesh.core.data.service;

import java.util.List;

import javax.annotation.PostConstruct;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.gentics.mesh.core.Page;
import com.gentics.mesh.core.data.MeshAuthUser;
import com.gentics.mesh.core.data.Role;
import com.gentics.mesh.core.data.impl.RoleImpl;
import com.gentics.mesh.paging.PagingInfo;
import com.gentics.mesh.util.InvalidArgumentException;
import com.gentics.mesh.util.TraversalHelper;
import com.syncleus.ferma.traversals.VertexTraversal;

@Component
public class RoleService extends AbstractMeshGraphService<Role> {

	@Autowired
	private UserService userService;

	public static RoleService instance;

	@PostConstruct
	public void setup() {
		instance = this;
	}

	public static RoleService getRoleService() {
		return instance;
	}

	public List<? extends Role> findAll() {
		return fg.v().has(RoleImpl.class).toListExplicit(RoleImpl.class);
	}

	public Page<? extends Role> findAll(MeshAuthUser requestUser, PagingInfo pagingInfo) throws InvalidArgumentException {
		// TODO filter for permissions
		VertexTraversal<?, ?, ?> traversal = fg.v().has(RoleImpl.class);
		VertexTraversal<?, ?, ?> countTraversal = fg.v().has(RoleImpl.class);
		return TraversalHelper.getPagedResult(traversal, countTraversal, pagingInfo, RoleImpl.class);
		// public Page<Role> findAll(String userUuid, Pageable pageable) {
		// // @Query(value = MATCH_PERMISSION_ON_ROLE + " WHERE " + FILTER_USER_PERM + "return role ORDER BY role.name",
		//
		// // countQuery = MATCH_PERMISSION_ON_ROLE + " WHERE " + FILTER_USER_PERM + " return count(role)")
		// return null;
		// }
		// TODO filter for permissions?
	}

	public Role findByName(String name) {
		return findByName(name, RoleImpl.class);
	}

	@Override
	public Role findByUUID(String uuid) {
		return findByUUID(uuid, RoleImpl.class);
	}

}
