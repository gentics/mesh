package com.gentics.mesh.core.data.root.impl;

import static com.gentics.mesh.core.data.relationship.MeshRelationships.HAS_PROJECT;

import com.gentics.mesh.core.data.Project;
import com.gentics.mesh.core.data.impl.ProjectImpl;
import com.gentics.mesh.core.data.root.ProjectRoot;
import com.gentics.mesh.core.data.root.SchemaContainerRoot;
import com.gentics.mesh.core.data.root.TagFamilyRoot;

public class ProjectRootImpl extends AbstractRootVertex<Project> implements ProjectRoot {

	@Override
	protected Class<? extends Project> getPersistanceClass() {
		return ProjectImpl.class;
	}

	@Override
	protected String getRootLabel() {
		return HAS_PROJECT;
	}

	@Override
	public void addProject(Project project) {
		addItem(project);
	}

	@Override
	public void removeProject(Project project) {
		removeItem(project);
	}

	// TODO unique

	@Override
	public Project create(String name) {
		Project project = getGraph().addFramedVertex(ProjectImpl.class);
		project.setName(name);
		project.getOrCreateRootNode();
		project.createTagRoot();
		
		SchemaContainerRoot schemaRoot = getGraph().addFramedVertex(SchemaContainerRootImpl.class);
		project.setSchemaRoot(schemaRoot);
		addItem(project);

		TagFamilyRoot tagFamilyRoot = project.createTagFamilyRoot();
		project.setTagFamilyRoot(tagFamilyRoot);
		return project;
	}

}
