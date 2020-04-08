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
import com.gentics.mesh.core.data.job.Job;
import com.gentics.mesh.core.data.job.JobRoot;
import com.gentics.mesh.core.data.page.TransformablePage;
import com.gentics.mesh.core.data.root.RootVertex;
import com.gentics.mesh.core.endpoint.handler.AbstractCrudHandler;
import com.gentics.mesh.core.rest.MeshEvent;
import com.gentics.mesh.core.rest.error.NotModifiedException;
import com.gentics.mesh.core.rest.job.JobResponse;
import com.gentics.mesh.core.rest.job.JobStatus;
import com.gentics.mesh.core.verticle.handler.HandlerUtilities;
import com.gentics.mesh.core.verticle.handler.GlobalLock;
import com.gentics.mesh.core.verticle.handler.GlobalLockImpl;
import com.gentics.mesh.graphdb.spi.Database;
import com.gentics.mesh.parameter.PagingParameters;

import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;

@Singleton
public class JobHandler extends AbstractCrudHandler<Job, JobResponse> {

	private static final Logger log = LoggerFactory.getLogger(JobHandler.class);

	private BootstrapInitializer boot;

	@Inject
	public JobHandler(Database db, BootstrapInitializer boot, HandlerUtilities utils, GlobalLockImpl writeLock) {
		super(db, utils, writeLock);
		this.boot = boot;

	}

	@Override
	public RootVertex<Job> getRootVertex(InternalActionContext ac) {
		return boot.jobRoot();
	}

	@Override
	public void handleReadList(InternalActionContext ac) {
		utils.syncTx(ac, tx -> {
			if (!ac.getUser().hasAdminRole()) {
				throw error(FORBIDDEN, "error_admin_permission_required");
			}
			JobRoot root = boot.jobRoot();

			PagingParameters pagingInfo = ac.getPagingParameters();
			TransformablePage<? extends Job> page = root.findAllNoPerm(ac, pagingInfo);

			// Handle etag
			if (ac.getGenericParameters().getETag()) {
				String etag = page.getETag(ac);
				ac.setEtag(etag, true);
				if (ac.matches(etag, true)) {
					throw new NotModifiedException();
				}
			}
			return page.transformToRestSync(ac, 0);
		}, e -> ac.send(e, OK));
	}

	@Override
	public void handleDelete(InternalActionContext ac, String uuid) {
		validateParameter(uuid, "uuid");

		try (GlobalLock lock = globalLock.writeLock(ac)) {
			utils.syncTx(ac, () -> {
				if (!ac.getUser().hasAdminRole()) {
					throw error(FORBIDDEN, "error_admin_permission_required");
				}
				JobRoot root = boot.jobRoot();
				Job job = root.loadObjectByUuidNoPerm(uuid, true);
				db.tx(() -> {
					if (job.hasFailed()) {
						job.delete();
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
		utils.syncTx(ac, (tx) -> {
			if (!ac.getUser().hasAdminRole()) {
				throw error(FORBIDDEN, "error_admin_permission_required");
			}
			JobRoot root = boot.jobRoot();
			Job job = root.loadObjectByUuidNoPerm(uuid, true);
			String etag = job.getETag(ac);
			ac.setEtag(etag, true);
			if (ac.matches(etag, true)) {
				throw new NotModifiedException();
			} else {
				return job.transformToRestSync(ac, 0);
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
		utils.syncTx(ac, (tx) -> {
			if (!ac.getUser().hasAdminRole()) {
				throw error(FORBIDDEN, "error_admin_permission_required");
			}
			JobRoot root = boot.jobRoot();
			Job job = root.loadObjectByUuidNoPerm(uuid, true);
			db.tx(() -> {
				job.resetJob();
			});
			return job.transformToRestSync(ac, 0);
		}, (model) -> ac.send(model, OK));
	}

	public void handleProcess(InternalActionContext ac, String uuid) {
		validateParameter(uuid, "uuid");
		try (GlobalLock lock = globalLock.writeLock(ac)) {
			utils.syncTx(ac, (tx) -> {
				if (!ac.getUser().hasAdminRole()) {
					throw error(FORBIDDEN, "error_admin_permission_required");
				}
				JobRoot root = boot.jobRoot();
				Job job = root.loadObjectByUuidNoPerm(uuid, true);
				db.tx(() -> {
					JobStatus status = job.getStatus();
					if (status == FAILED || status == UNKNOWN) {
						job.resetJob();
					}
				});
				MeshEvent.triggerJobWorker(boot.mesh());
				return job.transformToRestSync(ac, 0);
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
			if (!ac.getUser().hasAdminRole()) {
				throw error(FORBIDDEN, "error_admin_permission_required");
			}
			MeshEvent.triggerJobWorker(boot.mesh());
			return message(ac, "job_processing_invoked");
		}, model -> ac.send(model, OK));
	}
}
