package com.gentics.mesh.core.data.model.root;

import static com.gentics.mesh.core.data.model.relationship.MeshRelationships.HAS_PROJECT;

import java.util.List;

import com.gentics.mesh.core.data.model.generic.MeshVertex;
import com.gentics.mesh.core.data.model.tinkerpop.Project;

public class ProjectRoot extends MeshVertex {

	public List<? extends Project> getProjects() {
		return out(HAS_PROJECT).toList(Project.class);
	}

	public void addProject(Project project) {
		linkOut(project, HAS_PROJECT);
	}

	// TODO unique

	public Project create(String name) {
		Project project = getGraph().addFramedVertex(Project.class);
		project.setName(name);
		project.getOrCreateRootNode();
		SchemaRoot schemaRoot = getGraph().addFramedVertex(SchemaRoot.class);
		project.setSchemaRoot(schemaRoot);
		addProject(project);
		return project;
	}

}
