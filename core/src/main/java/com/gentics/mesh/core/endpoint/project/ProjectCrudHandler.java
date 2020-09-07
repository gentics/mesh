package com.gentics.mesh.core.endpoint.project;

import static com.gentics.mesh.core.data.perm.InternalPermission.DELETE_PERM;
import static com.gentics.mesh.core.data.perm.InternalPermission.READ_PERM;
import static com.gentics.mesh.core.rest.error.Errors.error;
import static com.gentics.mesh.rest.Messages.message;
import static io.netty.handler.codec.http.HttpResponseStatus.FORBIDDEN;
import static io.netty.handler.codec.http.HttpResponseStatus.OK;

import java.time.ZonedDateTime;
import java.util.Optional;

import javax.inject.Inject;

import com.gentics.mesh.cli.BootstrapInitializer;
import com.gentics.mesh.context.InternalActionContext;
import com.gentics.mesh.core.action.ProjectDAOActions;
import com.gentics.mesh.core.actions.impl.ProjectDAOActionsImpl;
import com.gentics.mesh.core.data.dao.ProjectDaoWrapper;
import com.gentics.mesh.core.data.project.HibProject;
import com.gentics.mesh.core.data.user.HibUser;
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
public class ProjectCrudHandler extends AbstractCrudHandler<HibProject, ProjectResponse> {

	private BootstrapInitializer boot;

	@Inject
	public ProjectCrudHandler(Database db, BootstrapInitializer boot, HandlerUtilities utils, WriteLock writeLock, ProjectDAOActions projectActions) {
		super(db, utils, writeLock, projectActions);
		this.boot = boot;
	}

	/**
	 * Handle a read project by name request.
	 * 
	 * @param ac
	 * @param projectName
	 *            Name of the project which should be read.
	 */
	public void handleReadByName(InternalActionContext ac, String projectName) {
		utils.syncTx(ac, tx -> {
			HibProject project = tx.projectDao().findByName(ac, projectName, READ_PERM);
			return crudActions().transformToRestSync(tx, project, ac, 0);
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

		try (WriteLock lock = writeLock.lock(ac)) {
			utils.syncTx(ac, tx -> {
				if (!ac.getUser().isAdmin()) {
					throw error(FORBIDDEN, "error_admin_permission_required");
				}
				HibUser user = ac.getUser();
				ProjectDaoWrapper projectDao = tx.projectDao();
				HibProject project = projectDao.loadObjectByUuid(ac, uuid, DELETE_PERM);
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
