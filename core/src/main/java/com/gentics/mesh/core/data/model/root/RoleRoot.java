package com.gentics.mesh.core.data.model.root;

import com.gentics.mesh.core.data.model.generic.MeshVertex;
import com.gentics.mesh.core.data.model.relationship.MeshRelationships;
import com.gentics.mesh.core.data.model.tinkerpop.Role;

public class RoleRoot extends MeshVertex {

	public Iterable<? extends Role> getRoles() {
		return out(MeshRelationships.HAS_ROLE).toList(Role.class);
	}
	
	public void addRole(Role role) {
		addFramedEdge(MeshRelationships.HAS_ROLE, role);
	}

	// TODO unique index

}
