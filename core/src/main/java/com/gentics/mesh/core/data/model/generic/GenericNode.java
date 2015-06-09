package com.gentics.mesh.core.data.model.generic;

import java.util.List;

import com.gentics.mesh.core.data.model.relationship.BasicRelationships;
import com.gentics.mesh.core.data.model.tinkerpop.Project;
import com.gentics.mesh.core.data.model.tinkerpop.User;
import com.tinkerpop.blueprints.Direction;

public class GenericNode extends AbstractPersistable {

	public List<Project> getProjects() {
		return out(BasicRelationships.ASSIGNED_TO_PROJECT).except(this).toList();
	}

	public void addProject(Project project) {
		addEdge(BasicRelationships.ASSIGNED_TO_PROJECT, project);
	}

	@Adjacency(label = BasicRelationships.ASSIGNED_TO_PROJECT, direction = Direction.OUT)
	public boolean removeProject(Project project);

	@Adjacency(label = BasicRelationships.HAS_CREATOR, direction = Direction.OUT)
	public User getCreator();

	@Adjacency(label = BasicRelationships.HAS_CREATOR, direction = Direction.OUT)
	public void setCreator(User user);

}
