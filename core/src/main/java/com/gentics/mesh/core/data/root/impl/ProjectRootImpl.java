package com.gentics.mesh.core.data.root.impl;

import static com.gentics.mesh.core.data.relationship.MeshRelationships.HAS_PROJECT;

import java.util.List;

import com.gentics.mesh.core.data.Project;
import com.gentics.mesh.core.data.generic.MeshVertexImpl;
import com.gentics.mesh.core.data.impl.ProjectImpl;
import com.gentics.mesh.core.data.root.ProjectRoot;
import com.gentics.mesh.core.data.root.SchemaContainerRoot;
import com.gentics.mesh.core.data.root.TagFamilyRoot;

public class ProjectRootImpl extends MeshVertexImpl implements ProjectRoot {

	public List<? extends Project> getProjects() {
		return out(HAS_PROJECT).toList(ProjectImpl.class);
	}

	public void addProject(Project project) {
		linkOut(project.getImpl(), HAS_PROJECT);
	}

	// TODO unique

	public Project create(String name) {
		Project project = getGraph().addFramedVertex(ProjectImpl.class);
		project.setName(name);
		project.getOrCreateRootNode();
		SchemaContainerRoot schemaRoot = getGraph().addFramedVertex(SchemaRootImpl.class);
		project.setSchemaRoot(schemaRoot);
		addProject(project);

		TagFamilyRoot tagFamilyRoot = project.createTagFamilyRoot();
		project.setTagFamilyRoot(tagFamilyRoot);
		return project;
	}

	@Override
	public ProjectRootImpl getImpl() {
		return this;
	}

}
