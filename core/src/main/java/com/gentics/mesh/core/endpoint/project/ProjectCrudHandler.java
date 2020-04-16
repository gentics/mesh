package com.gentics.mesh.core.endpoint.project;

import static com.gentics.mesh.core.data.relationship.GraphPermission.DELETE_PERM;
import static com.gentics.mesh.core.data.relationship.GraphPermission.READ_PERM;
import static com.gentics.mesh.core.rest.error.Errors.error;
import static com.gentics.mesh.rest.Messages.message;
import static io.netty.handler.codec.http.HttpResponseStatus.FORBIDDEN;
import static io.netty.handler.codec.http.HttpResponseStatus.OK;

import java.time.ZonedDateTime;
import java.util.Optional;

import javax.inject.Inject;

import com.gentics.mesh.cli.BootstrapInitializer;
import com.gentics.mesh.context.InternalActionContext;
import com.gentics.mesh.core.data.MeshAuthUser;
import com.gentics.mesh.core.data.Project;
import com.gentics.mesh.core.data.root.RootVertex;
import com.gentics.mesh.core.endpoint.handler.AbstractCrudHandler;
import com.gentics.mesh.core.rest.MeshEvent;
import com.gentics.mesh.core.rest.project.ProjectResponse;
import com.gentics.mesh.core.verticle.handler.HandlerUtilities;
import com.gentics.mesh.core.verticle.handler.WriteLock;
import com.gentics.mesh.graphdb.spi.Database;
import com.gentics.mesh.parameter.ProjectPurgeParameters;

/**
 * Handler for project specific requests.
 */
public class ProjectCrudHandler extends AbstractCrudHandler<Project, ProjectResponse> {

	private BootstrapInitializer boot;

	@Inject
	public ProjectCrudHandler(Database db, BootstrapInitializer boot, HandlerUtilities utils, WriteLock writeLock) {
		super(db, utils, writeLock);
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

		ProjectPurgeParameters purgeParams = ac.getProjectPurgeParameters();
		Optional<ZonedDateTime> before = purgeParams.getBeforeDate();

		try (WriteLock lock = globalLock.lock(ac)) {
			utils.syncTx(ac, (tx) -> {
				if (!ac.getUser().hasAdminRole()) {
					throw error(FORBIDDEN, "error_admin_permission_required");
				}
				RootVertex<Project> root = getRootVertex(ac);
				MeshAuthUser user = ac.getUser();
				Project project = root.loadObjectByUuid(ac, uuid, DELETE_PERM);
				db.tx(() -> {
					if (before.isPresent()) {
						boot.jobRoot().enqueueVersionPurge(user, project, before.get());
					} else {
						boot.jobRoot().enqueueVersionPurge(user, project);
					}
				});
				MeshEvent.triggerJobWorker(boot.mesh());

				return message(ac, "project_version_purge_enqueued");
			}, message -> ac.send(message, OK));
		}
	}

}
