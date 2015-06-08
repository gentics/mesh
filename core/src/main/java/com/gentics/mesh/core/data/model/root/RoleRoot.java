package com.gentics.mesh.core.data.model.root;

import com.gentics.mesh.core.data.model.generic.AbstractPersistable;
import com.gentics.mesh.core.data.model.relationship.BasicRelationships;
import com.gentics.mesh.core.data.model.tinkerpop.Role;
import com.tinkerpop.blueprints.Direction;
import com.tinkerpop.frames.Adjacency;

public interface RoleRoot extends AbstractPersistable {

	@Adjacency(label = BasicRelationships.HAS_ROLE, direction = Direction.OUT)
	public Iterable<Role> getRoles();

	// TODO unique index

}
