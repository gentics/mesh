package com.gentics.mesh.core.data.root.impl;

import static com.gentics.mesh.core.data.relationship.MeshRelationships.HAS_ROLE;

import com.gentics.mesh.core.Page;
import com.gentics.mesh.core.data.MeshAuthUser;
import com.gentics.mesh.core.data.Role;
import com.gentics.mesh.core.data.impl.RoleImpl;
import com.gentics.mesh.core.data.root.RoleRoot;
import com.gentics.mesh.paging.PagingInfo;
import com.gentics.mesh.util.InvalidArgumentException;
import com.gentics.mesh.util.TraversalHelper;
import com.syncleus.ferma.traversals.VertexTraversal;

public class RoleRootImpl extends AbstractRootVertex<Role> implements RoleRoot {

	@Override
	protected Class<? extends Role> getPersistanceClass() {
		return RoleImpl.class;
	}

	@Override
	protected String getRootLabel() {
		return HAS_ROLE;
	}

	@Override
	public void addRole(Role role) {
		linkOut(role.getImpl(), HAS_ROLE);
	}

	@Override
	public void removeRole(Role role) {
		// TODO delete the role? unlink from all groups? how is ferma / blueprint handling this. Neo4j would explode when trying to remove a node that still has
		// connecting edges.
		unlinkOut(role.getImpl(), HAS_ROLE);
	}

	// TODO unique index

	@Override
	public Role create(String name) {
		Role role = getGraph().addFramedVertex(RoleImpl.class);
		role.setName(name);
		addRole(role);
		return role;
	}

	@Override
	public RoleRootImpl getImpl() {
		return this;
	}

	// @Override
	// public Page<? extends Role> findAll(MeshAuthUser requestUser, PagingInfo pagingInfo) throws InvalidArgumentException {
	// // TODO filter for permissions
	// VertexTraversal<?, ?, ?> traversal = out(HAS_ROLE).has(RoleImpl.class);
	// VertexTraversal<?, ?, ?> countTraversal = out(HAS_ROLE).has(RoleImpl.class);
	// return TraversalHelper.getPagedResult(traversal, countTraversal, pagingInfo, RoleImpl.class);
	// // public Page<Role> findAll(String userUuid, Pageable pageable) {
	// // // @Query(value = MATCH_PERMISSION_ON_ROLE + " WHERE " + FILTER_USER_PERM + "return role ORDER BY role.name",
	// //
	// // // countQuery = MATCH_PERMISSION_ON_ROLE + " WHERE " + FILTER_USER_PERM + " return count(role)")
	// // return null;
	// // }
	// // TODO filter for permissions?
	// }

}
