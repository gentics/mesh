package com.gentics.mesh.core.endpoint.project;

import static com.gentics.mesh.core.data.relationship.GraphPermission.DELETE_PERM;
import static com.gentics.mesh.core.data.relationship.GraphPermission.READ_PERM;
import static com.gentics.mesh.rest.Messages.message;
import static io.netty.handler.codec.http.HttpResponseStatus.OK;

import javax.inject.Inject;

import com.gentics.mesh.cli.BootstrapInitializer;
import com.gentics.mesh.context.InternalActionContext;
import com.gentics.mesh.core.data.MeshAuthUser;
import com.gentics.mesh.core.data.Project;
import com.gentics.mesh.core.data.job.Job;
import com.gentics.mesh.core.data.root.RootVertex;
import com.gentics.mesh.core.endpoint.handler.AbstractCrudHandler;
import com.gentics.mesh.core.rest.MeshEvent;
import com.gentics.mesh.core.rest.project.ProjectResponse;
import com.gentics.mesh.core.verticle.handler.HandlerUtilities;
import com.gentics.mesh.graphdb.spi.Database;

/**
 * Handler for project specific requests.
 */
public class ProjectCrudHandler extends AbstractCrudHandler<Project, ProjectResponse> {

	private BootstrapInitializer boot;

	@Inject
	public ProjectCrudHandler(Database db, BootstrapInitializer boot, HandlerUtilities utils) {
		super(db, utils);
		this.boot = boot;
	}

	@Override
	public RootVertex<Project> getRootVertex(InternalActionContext ac) {
		return boot.projectRoot();
	}

	/**
	 * Handle a read project by name request.
	 * 
	 * @param ac
	 * @param projectName
	 *            Name of the project which should be read.
	 */
	public void handleReadByName(InternalActionContext ac, String projectName) {
		utils.syncTx(ac, (tx) -> {
			RootVertex<Project> root = getRootVertex(ac);
			Project project = root.findByName(ac, projectName, READ_PERM);
			return project.transformToRestSync(ac, 0);
		}, model -> ac.send(model, OK));
	}

	/**
	 * Handle the project purge request.
	 * 
	 * @param ac
	 * @param uuid
	 */
	public void handlePurge(InternalActionContext ac, String uuid) {
		validateParameter(uuid, "uuid");

		utils.syncTx(ac, (tx) -> {
			RootVertex<Project> root = getRootVertex(ac);
			MeshAuthUser user = ac.getUser();
			// TODO which perm to use? Admin perm?
			Project project = root.loadObjectByUuid(ac, uuid, DELETE_PERM);
			db.tx(() -> {
				Job job = boot.jobRoot().enqueueVersionPurge(user, project);
			});
			MeshEvent.triggerJobWorker();

			return message(ac, "project_version_purge_enqueued");
		}, message -> ac.send(message, OK));

	}

}
