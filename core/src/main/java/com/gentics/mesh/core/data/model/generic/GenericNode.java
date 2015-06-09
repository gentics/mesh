package com.gentics.mesh.core.data.model.generic;

import java.util.List;

import com.gentics.mesh.core.data.model.relationship.BasicRelationships;
import com.gentics.mesh.core.data.model.tinkerpop.Project;
import com.gentics.mesh.core.data.model.tinkerpop.User;
import com.tinkerpop.blueprints.Direction;

public class GenericNode extends AbstractPersistable {

	public List<Project> getProjects() {
		return out(BasicRelationships.ASSIGNED_TO_PROJECT).toList(Project.class);
	}

	public void addProject(Project project) {
		addEdge(BasicRelationships.ASSIGNED_TO_PROJECT, project);
	}

	public void removeProject(Project project) {
		unlinkOut(project, BasicRelationships.ASSIGNED_TO_PROJECT);
	}

	// @Adjacency(label = BasicRelationships.HAS_CREATOR, direction = Direction.OUT)
	public User getCreator() {
		return out(BasicRelationships.HAS_CREATOR).next(User.class);
	}

	// @Adjacency(label = BasicRelationships.HAS_CREATOR, direction = Direction.OUT)
	public void setCreator(User user) {

	}

}
