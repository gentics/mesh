package com.gentics.mesh.core.data.model.root;

import static com.gentics.mesh.core.data.model.relationship.MeshRelationships.HAS_ROLE;

import java.util.List;

import com.gentics.mesh.core.data.model.generic.MeshVertex;
import com.gentics.mesh.core.data.model.tinkerpop.Role;

public class RoleRoot extends MeshVertex {

	public List<? extends Role> getRoles() {
		return out(HAS_ROLE).has(Role.class).toListExplicit(Role.class);
	}

	public void addRole(Role role) {
		linkOut(role, HAS_ROLE);
	}

	public void removeRole(Role role) {
		//TODO delete the role?  unlink from all groups? how is ferma / blueprint handling this. Neo4j would explode when trying to remove a node that still has connecting edges.
		unlinkOut(role, HAS_ROLE);
	}

	// TODO unique index

	public Role create(String name) {
		Role role = getGraph().addFramedVertex(Role.class);
		role.setName(name);
		addRole(role);
		return role;
	}

}
