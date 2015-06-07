package com.gentics.mesh.core.data.model.tinkerpop;

import com.gentics.mesh.core.data.model.relationship.BasicRelationships;
import com.tinkerpop.blueprints.Direction;
import com.tinkerpop.frames.Adjacency;

public interface TPProjectRoot extends TPAbstractPersistable {

	@Adjacency(label = BasicRelationships.HAS_PROJECT, direction = Direction.OUT)
	public Iterable<TPProject> getProjects();

}
