package com.gentics.mesh.core.data.model.tinkerpop;

import com.gentics.mesh.core.data.model.relationship.BasicRelationships;
import com.tinkerpop.blueprints.Direction;
import com.tinkerpop.frames.Adjacency;

public interface TPRoleRoot extends TPAbstractPersistable {

	@Adjacency(label = BasicRelationships.HAS_ROLE, direction = Direction.OUT)
	public Iterable<TPRole> getRoles();

	// TODO unique index

}
