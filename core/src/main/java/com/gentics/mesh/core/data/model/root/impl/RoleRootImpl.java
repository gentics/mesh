package com.gentics.mesh.core.data.model.root.impl;

import static com.gentics.mesh.core.data.model.relationship.MeshRelationships.HAS_ROLE;

import java.util.List;

import com.gentics.mesh.core.data.model.Role;
import com.gentics.mesh.core.data.model.generic.MeshVertexImpl;
import com.gentics.mesh.core.data.model.impl.RoleImpl;
import com.gentics.mesh.core.data.model.root.RoleRoot;

public class RoleRootImpl extends MeshVertexImpl implements RoleRoot {

	public List<? extends Role> getRoles() {
		return out(HAS_ROLE).has(RoleImpl.class).toListExplicit(RoleImpl.class);
	}

	public void addRole(Role role) {
		linkOut((RoleImpl) role, HAS_ROLE);
	}

	public void removeRole(RoleImpl role) {
		// TODO delete the role? unlink from all groups? how is ferma / blueprint handling this. Neo4j would explode when trying to remove a node that still has
		// connecting edges.
		unlinkOut(role, HAS_ROLE);
	}

	// TODO unique index

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

}
