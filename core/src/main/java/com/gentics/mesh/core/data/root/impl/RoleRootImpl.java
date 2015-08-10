package com.gentics.mesh.core.data.root.impl;

import static com.gentics.mesh.core.data.relationship.MeshRelationships.HAS_ROLE;

import com.gentics.mesh.core.data.Group;
import com.gentics.mesh.core.data.Role;
import com.gentics.mesh.core.data.User;
import com.gentics.mesh.core.data.impl.RoleImpl;
import com.gentics.mesh.core.data.root.RoleRoot;

public class RoleRootImpl extends AbstractRootVertex<Role>implements RoleRoot {

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
		addItem(role);
	}

	@Override
	public void removeRole(Role role) {
		// TODO delete the role? unlink from all groups? how is ferma / blueprint handling this. Neo4j would explode when trying to remove a node that still has
		// connecting edges.
		removeItem(role);
	}

	// TODO unique index

	@Override
	public Role create(String name, Group group, User creator) {
		Role role = getGraph().addFramedVertex(RoleImpl.class);
		role.setName(name);
		role.setCreator(creator);
		role.setCreationTimestamp(System.currentTimeMillis());
		role.setEditor(creator);
		role.setLastEditedTimestamp(System.currentTimeMillis());
		addRole(role);
		if (group != null) {
			group.addRole(role);
		}
		return role;
	}

}
