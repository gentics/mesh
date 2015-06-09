package com.gentics.mesh.core.data.model.root;

import java.util.List;

import com.gentics.mesh.core.data.model.generic.AbstractPersistable;
import com.gentics.mesh.core.data.model.relationship.BasicRelationships;
import com.gentics.mesh.core.data.model.tinkerpop.Project;

public class ProjectRoot extends AbstractPersistable {

	//	@Adjacency(label = BasicRelationships.HAS_PROJECT, direction = Direction.OUT)
	public List<Project> getProjects() {
		return out(BasicRelationships.HAS_PROJECT).toList(Project.class);
	}

	//TODO unique

}
