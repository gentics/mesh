package com.gentics.mesh.core.data.model.generic;

import com.gentics.mesh.core.data.model.relationship.BasicRelationships;
import com.gentics.mesh.core.data.model.tinkerpop.Project;
import com.gentics.mesh.core.data.model.tinkerpop.User;
import com.tinkerpop.blueprints.Direction;
import com.tinkerpop.frames.Adjacency;

public interface GenericNode extends AbstractPersistable {

	@Adjacency(label = BasicRelationships.ASSIGNED_TO_PROJECT, direction = Direction.OUT)
	public Iterable<Project> getProjects();

	@Adjacency(label = BasicRelationships.ASSIGNED_TO_PROJECT, direction = Direction.OUT)
	public void addProject(Project project);

	@Adjacency(label = BasicRelationships.ASSIGNED_TO_PROJECT, direction = Direction.OUT)
	public boolean removeProject(Project project);

	@Adjacency(label = BasicRelationships.HAS_CREATOR, direction = Direction.OUT)
	public User getCreator();

	@Adjacency(label = BasicRelationships.HAS_CREATOR, direction = Direction.OUT)
	public void setCreator(User user);

}
