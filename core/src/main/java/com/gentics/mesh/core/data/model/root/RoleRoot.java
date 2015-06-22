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
		addFramedEdge(HAS_ROLE, role);
	}

	// TODO unique index

}
