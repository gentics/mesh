package com.gentics.mesh.core.data.model.tinkerpop;

import com.gentics.mesh.core.data.model.relationship.BasicRelationships;
import com.tinkerpop.blueprints.Direction;
import com.tinkerpop.frames.Adjacency;

public interface TPGenericNode extends TPAbstractPersistable {

	@Adjacency(label = BasicRelationships.ASSIGNED_TO_PROJECT, direction = Direction.OUT)
	public Iterable<TPProject> getProjects();

	@Adjacency(label = BasicRelationships.ASSIGNED_TO_PROJECT, direction = Direction.OUT)
	public void addProject(TPProject project);

	@Adjacency(label = BasicRelationships.HAS_CREATOR, direction = Direction.OUT)
	public TPUser getCreator();

	@Adjacency(label = BasicRelationships.HAS_CREATOR, direction = Direction.OUT)
	public void setCreator(TPUser user);

}
