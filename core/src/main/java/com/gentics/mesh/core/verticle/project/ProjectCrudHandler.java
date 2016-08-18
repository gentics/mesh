package com.gentics.mesh.core.verticle.project;

import static com.gentics.mesh.core.data.relationship.GraphPermission.READ_PERM;
import static io.netty.handler.codec.http.HttpResponseStatus.OK;

import org.springframework.stereotype.Component;

import com.gentics.mesh.context.InternalActionContext;
import com.gentics.mesh.core.data.Project;
import com.gentics.mesh.core.data.root.RootVertex;
import com.gentics.mesh.core.rest.project.ProjectResponse;
import com.gentics.mesh.core.verticle.handler.AbstractCrudHandler;
import com.gentics.mesh.core.verticle.handler.HandlerUtilities;
import com.gentics.mesh.etc.MeshSpringConfiguration;
import com.gentics.mesh.graphdb.spi.Database;

@Component
public class ProjectCrudHandler extends AbstractCrudHandler<Project, ProjectResponse> {

	@Override
	public RootVertex<Project> getRootVertex(InternalActionContext ac) {
		return boot.projectRoot();
	}

	@Override
	public void handleDelete(InternalActionContext ac, String uuid) {
		validateParameter(uuid, "uuid");
		HandlerUtilities.deleteElement(ac, () -> getRootVertex(ac), uuid, "project_deleted");
	}

	public void handleReadByName(InternalActionContext ac, String projectName) {
		Database db = MeshSpringConfiguration.getInstance().database();
		db.asyncNoTx(() -> {
			RootVertex<Project> root = getRootVertex(ac);
			return root.findByName(ac, projectName, READ_PERM).flatMap(project -> {
				return project.transformToRest(ac, 0);
			});
		}).subscribe(model -> ac.send(model, OK), ac::fail);
	}

}
