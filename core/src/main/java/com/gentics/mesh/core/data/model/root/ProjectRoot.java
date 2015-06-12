package com.gentics.mesh.core.data.model.root;

import java.util.List;

import com.gentics.mesh.core.data.model.generic.MeshVertex;
import com.gentics.mesh.core.data.model.relationship.BasicRelationships;
import com.gentics.mesh.core.data.model.tinkerpop.Project;

public class ProjectRoot extends MeshVertex {

	public List<? extends Project> getProjects() {
		return out(BasicRelationships.HAS_PROJECT).toList(Project.class);
	}

	//TODO unique

}
