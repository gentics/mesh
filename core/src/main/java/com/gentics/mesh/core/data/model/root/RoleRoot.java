package com.gentics.mesh.core.data.model.root;

import com.gentics.mesh.core.data.model.generic.AbstractPersistable;
import com.gentics.mesh.core.data.model.relationship.BasicRelationships;
import com.gentics.mesh.core.data.model.tinkerpop.Role;

public class RoleRoot extends AbstractPersistable {

	public Iterable<Role> getRoles() {
		return out(BasicRelationships.HAS_ROLE).toList(Role.class);
	}

	// TODO unique index

}
