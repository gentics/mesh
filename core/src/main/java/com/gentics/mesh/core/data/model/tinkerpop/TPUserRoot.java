package com.gentics.mesh.core.data.model.tinkerpop;

import com.gentics.mesh.core.data.model.relationship.BasicRelationships;
import com.tinkerpop.blueprints.Direction;
import com.tinkerpop.frames.Adjacency;

public interface TPUserRoot extends TPAbstractPersistable {

	@Adjacency(label = BasicRelationships.HAS_USER, direction = Direction.OUT)
	public Iterable<TPUser> getUsers();

	// TODO unique index

}
