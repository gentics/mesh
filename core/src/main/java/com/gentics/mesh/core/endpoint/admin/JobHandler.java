package com.gentics.mesh.core.endpoint.admin;

import static com.gentics.mesh.core.rest.error.Errors.error;
import static com.gentics.mesh.core.rest.job.JobStatus.FAILED;
import static com.gentics.mesh.core.rest.job.JobStatus.UNKNOWN;
import static com.gentics.mesh.rest.Messages.message;
import static io.netty.handler.codec.http.HttpResponseStatus.BAD_REQUEST;
import static io.netty.handler.codec.http.HttpResponseStatus.FORBIDDEN;
import static io.netty.handler.codec.http.HttpResponseStatus.NO_CONTENT;
import static io.netty.handler.codec.http.HttpResponseStatus.OK;

import javax.inject.Inject;
import javax.inject.Singleton;

import org.apache.commons.lang3.NotImplementedException;

import com.gentics.mesh.cli.BootstrapInitializer;
import com.gentics.mesh.context.InternalActionContext;
import com.gentics.mesh.context.impl.DummyBulkActionContext;
import com.gentics.mesh.core.action.JobDAOActions;
import com.gentics.mesh.core.data.dao.JobDao;
import com.gentics.mesh.core.data.job.HibJob;
import com.gentics.mesh.core.data.page.Page;
import com.gentics.mesh.core.data.page.PageTransformer;
import com.gentics.mesh.core.endpoint.handler.AbstractCrudHandler;
import com.gentics.mesh.core.rest.MeshEvent;
import com.gentics.mesh.core.rest.error.NotModifiedException;
import com.gentics.mesh.core.rest.job.JobResponse;
import com.gentics.mesh.core.rest.job.JobStatus;
import com.gentics.mesh.core.verticle.handler.HandlerUtilities;
import com.gentics.mesh.core.verticle.handler.WriteLock;
import com.gentics.mesh.graphdb.spi.Database;
import com.gentics.mesh.parameter.PagingParameters;

import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;

/**
 * REST handler for job endpoint operations.
 */
@Singleton
public class JobHandler extends AbstractCrudHandler<HibJob, JobResponse> {

	private static final Logger log = LoggerFactory.getLogger(JobHandler.class);

	private BootstrapInitializer boot;

	private final PageTransformer pageTransformer;

	@Inject
	public JobHandler(Database db, BootstrapInitializer boot, HandlerUtilities utils, WriteLock writeLock, JobDAOActions jobActions,
		PageTransformer pageTransformer) {
		super(db, utils, writeLock, jobActions);
		this.boot = boot;
		this.pageTransformer = pageTransformer;
	}

	@Override
	public void handleReadList(InternalActionContext ac) {
		utils.syncTx(ac, tx -> {
			if (!ac.getUser().isAdmin()) {
				throw error(FORBIDDEN, "error_admin_permission_required");
			}
			JobDao root = boot.jobDao();

			PagingParameters pagingInfo = ac.getPagingParameters();
			Page<? extends HibJob> page = root.findAllNoPerm(ac, pagingInfo);

			// Handle etag
			if (ac.getGenericParameters().getETag()) {
				String etag = pageTransformer.getETag(page, ac);
				ac.setEtag(etag, true);
				if (ac.matches(etag, true)) {
					throw new NotModifiedException();
				}
			}
			return pageTransformer.transformToRestSync(page, ac, 0);
		}, e -> ac.send(e, OK));
	}

	@Override
	public void handleDelete(InternalActionContext ac, String uuid) {
		validateParameter(uuid, "uuid");

		try (WriteLock lock = writeLock.lock(ac)) {
			utils.syncTx(ac, () -> {
				if (!ac.getUser().isAdmin()) {
					throw error(FORBIDDEN, "error_admin_permission_required");
				}
				JobDao root = boot.jobDao();
				HibJob job = root.loadObjectByUuidNoPerm(uuid, true);
				db.tx(() -> {
					if (job.hasFailed()) {
						root.delete(job, new DummyBulkActionContext());
					} else {
						throw error(BAD_REQUEST, "job_error_invalid_state", uuid);
					}
				});
				log.info("Deleted job {" + uuid + "}");
			}, () -> ac.send(NO_CONTENT));
		}

	}

	@Override
	public void handleRead(InternalActionContext ac, String uuid) {
		validateParameter(uuid, "uuid");
		utils.syncTx(ac, tx -> {
			if (!ac.getUser().isAdmin()) {
				throw error(FORBIDDEN, "error_admin_permission_required");
			}
			JobDao jobDao = tx.jobDao();
			HibJob job = jobDao.loadObjectByUuidNoPerm(uuid, true);
			String etag = jobDao.getETag(job, ac);
			ac.setEtag(etag, true);
			if (ac.matches(etag, true)) {
				throw new NotModifiedException();
			} else {
				return jobDao.transformToRestSync(job, ac, 0);
			}
		}, model -> ac.send(model, OK));
	}

	@Override
	public void handleCreate(InternalActionContext ac) {
		throw new NotImplementedException("Jobs can only be enqueued by internal processes.");
	}

	/**
	 * Reset the given job and remove the error state from the job.
	 * 
	 * @param ac
	 * @param uuid
	 */
	public void handleResetJob(InternalActionContext ac, String uuid) {
		utils.syncTx(ac, tx -> {
			if (!ac.getUser().isAdmin()) {
				throw error(FORBIDDEN, "error_admin_permission_required");
			}
			JobDao root = boot.jobDao();
			HibJob job = root.loadObjectByUuidNoPerm(uuid, true);
			db.tx(() -> {
				job.resetJob();
			});
			return root.transformToRestSync(job, ac, 0);
		}, (model) -> ac.send(model, OK));
	}

	/**
	 * Invoke the job processing.
	 * 
	 * @param ac
	 * @param uuid
	 */
	public void handleProcess(InternalActionContext ac, String uuid) {
		validateParameter(uuid, "uuid");
		try (WriteLock lock = writeLock.lock(ac)) {
			utils.syncTx(ac, (tx) -> {
				if (!ac.getUser().isAdmin()) {
					throw error(FORBIDDEN, "error_admin_permission_required");
				}
				JobDao root = boot.jobDao();
				HibJob job = root.loadObjectByUuidNoPerm(uuid, true);
				db.tx(() -> {
					JobStatus status = job.getStatus();
					if (status == FAILED || status == UNKNOWN) {
						job.resetJob();
					}
				});
				MeshEvent.triggerJobWorker(boot.mesh());
				return root.transformToRestSync(job, ac, 0);
			}, model -> ac.send(model, OK));
		}
	}

	/**
	 * Invoke the job worker verticle.
	 * 
	 * @param ac
	 */
	public void handleInvokeJobWorker(InternalActionContext ac) {
		utils.syncTx(ac, (tx) -> {
			if (!ac.getUser().isAdmin()) {
				throw error(FORBIDDEN, "error_admin_permission_required");
			}
			MeshEvent.triggerJobWorker(boot.mesh());
			return message(ac, "job_processing_invoked");
		}, model -> ac.send(model, OK));
	}
}
