package com.gentics.mesh.core.verticle.project;

import org.springframework.stereotype.Component;

import com.gentics.mesh.core.data.Project;
import com.gentics.mesh.core.data.root.RootVertex;
import com.gentics.mesh.core.verticle.handler.AbstractCrudHandler;
import com.gentics.mesh.handler.InternalActionContext;

@Component
public class ProjectCrudHandler extends AbstractCrudHandler<Project> {

	@Override
	public RootVertex<Project> getRootVertex(InternalActionContext ac) {
		return boot.projectRoot();
	}

	@Override
	public void handleDelete(InternalActionContext ac) {
		deleteElement(ac, () -> getRootVertex(ac), "uuid", "project_deleted");
	}

}
