package com.gentics.mesh.assertj.impl;

import static com.gentics.mesh.assertj.MeshAssertions.assertThat;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import com.gentics.mesh.assertj.AbstractMeshAssert;
import com.gentics.mesh.core.data.Project;
import com.gentics.mesh.core.rest.project.ProjectCreateRequest;
import com.gentics.mesh.core.rest.project.ProjectResponse;

public class ProjectResponseAssert extends AbstractMeshAssert<ProjectResponseAssert, ProjectResponse> {

	public ProjectResponseAssert(ProjectResponse actual) {
		super(actual, ProjectResponseAssert.class);
	}

	public ProjectResponseAssert matches(Project project) {
		assertGenericNode(project, actual);
		assertNotNull(actual.getRootNode());
		assertEquals(project.getName(), actual.getName());
		assertNotNull(actual.getRootNode());
		assertEquals(project.getBaseNode().getUuid(), actual.getRootNode().getUuid());
		return this;
	}

	public ProjectResponseAssert hasUuid(String uuid) {
		assertThat(actual.getUuid()).as("Uuid").isEqualTo(uuid);
		return this;
	}

	public ProjectResponseAssert matches(ProjectCreateRequest request) {
		assertNotNull(request);
		assertNotNull(actual);
		assertEquals(request.getName(), actual.getName());
		assertNotNull(actual.getUuid());
		assertNotNull(actual.getPermissions());
		return this;
	}

}
