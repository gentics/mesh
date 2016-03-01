package com.gentics.mesh.core.verticle.project;

import org.springframework.stereotype.Component;

import com.gentics.mesh.context.InternalActionContext;
import com.gentics.mesh.core.data.Project;
import com.gentics.mesh.core.data.root.RootVertex;
import com.gentics.mesh.core.rest.project.ProjectResponse;
import com.gentics.mesh.core.verticle.handler.AbstractCrudHandler;

@Component
public class ProjectCrudHandler extends AbstractCrudHandler<Project, ProjectResponse> {

	@Override
	public RootVertex<Project> getRootVertex(InternalActionContext ac) {
		return boot.projectRoot();
	}

	@Override
	public void handleDelete(InternalActionContext ac, String uuid) {
		validateParameter(uuid, "uuid");
		deleteElement(ac, () -> getRootVertex(ac), uuid, "project_deleted");
	}

}
