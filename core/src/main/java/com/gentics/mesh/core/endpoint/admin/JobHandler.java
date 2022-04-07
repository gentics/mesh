package com.gentics.mesh.core.endpoint.admin;

import static com.gentics.mesh.core.rest.error.Errors.error;
import static com.gentics.mesh.core.rest.job.JobStatus.FAILED;
import static com.gentics.mesh.core.rest.job.JobStatus.UNKNOWN;
import static com.gentics.mesh.rest.Messages.message;
import static io.netty.handler.codec.http.HttpResponseStatus.BAD_REQUEST;
import static io.netty.handler.codec.http.HttpResponseStatus.FORBIDDEN;
import static io.netty.handler.codec.http.HttpResponseStatus.NO_CONTENT;
import static io.netty.handler.codec.http.HttpResponseStatus.OK;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Predicate;

import javax.inject.Inject;
import javax.inject.Singleton;

import org.apache.commons.lang3.NotImplementedException;
import org.apache.commons.lang3.tuple.Pair;

import com.gentics.mesh.cli.BootstrapInitializer;
import com.gentics.mesh.context.InternalActionContext;
import com.gentics.mesh.context.impl.DummyBulkActionContext;
import com.gentics.mesh.core.action.JobDAOActions;
import com.gentics.mesh.core.data.branch.HibBranch;
import com.gentics.mesh.core.data.dao.JobDao;
import com.gentics.mesh.core.data.job.HibJob;
import com.gentics.mesh.core.data.page.Page;
import com.gentics.mesh.core.data.page.PageTransformer;
import com.gentics.mesh.core.data.schema.HibMicroschemaVersion;
import com.gentics.mesh.core.data.schema.HibSchemaVersion;
import com.gentics.mesh.core.db.Database;
import com.gentics.mesh.core.endpoint.handler.AbstractCrudHandler;
import com.gentics.mesh.core.rest.MeshEvent;
import com.gentics.mesh.core.rest.error.NotModifiedException;
import com.gentics.mesh.core.rest.job.JobResponse;
import com.gentics.mesh.core.rest.job.JobStatus;
import com.gentics.mesh.core.verticle.handler.HandlerUtilities;
import com.gentics.mesh.core.verticle.handler.WriteLock;
import com.gentics.mesh.parameter.JobParameters;
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
			JobParameters jobParameters = ac.getJobParameters();
			Predicate<HibJob> filter = null;
			if (!jobParameters.isEmpty()) {
				List<Pair<Set<String>, Function<HibJob, String>>> props = new ArrayList<>();
				props.add(Pair.of(jobParameters.getBranchName(), job -> {
					HibBranch branch = job.getBranch();
					return branch != null ? branch.getName() : null;
				}));
				props.add(Pair.of(jobParameters.getBranchUuid(), job -> {
					HibBranch branch = job.getBranch();
					return branch != null ? branch.getUuid() : null;
				}));
				props.add(Pair.of(jobParameters.getSchemaName(), job -> {
					HibSchemaVersion toSchemaVersion = job.getToSchemaVersion();
					if (toSchemaVersion != null) {
						return toSchemaVersion.getSchemaContainer().getName();
					} else {
						return null;
					}
				}));
				props.add(Pair.of(jobParameters.getSchemaUuid(), job -> {
					HibSchemaVersion toSchemaVersion = job.getToSchemaVersion();
					if (toSchemaVersion != null) {
						return toSchemaVersion.getSchemaContainer().getUuid();
					} else {
						return null;
					}
				}));
				props.add(Pair.of(jobParameters.getMicroschemaName(), job -> {
					HibMicroschemaVersion toVersion = job.getToMicroschemaVersion();
					if (toVersion != null) {
						return toVersion.getSchemaContainer().getName();
					} else {
						return null;
					}
				}));
				props.add(Pair.of(jobParameters.getMicroschemaUuid(), job -> {
					HibMicroschemaVersion toVersion = job.getToMicroschemaVersion();
					if (toVersion != null) {
						return toVersion.getSchemaContainer().getUuid();
					} else {
						return null;
					}
				}));
				props.add(Pair.of(jobParameters.getFromVersion(), job -> {
					if (job.getFromSchemaVersion() != null) {
						return job.getFromSchemaVersion().getVersion();
					} else if (job.getFromMicroschemaVersion() != null) {
						return job.getFromMicroschemaVersion().getVersion();
					}
					return null;
				}));
				props.add(Pair.of(jobParameters.getToVersion(), job -> {
					if (job.getToSchemaVersion() != null) {
						return job.getToSchemaVersion().getVersion();
					} else if (job.getToMicroschemaVersion() != null) {
						return job.getToMicroschemaVersion().getVersion();
					}
					return null;
				}));
				filter = job -> {
					if (!jobParameters.getStatus().isEmpty() && !jobParameters.getStatus().contains(job.getStatus())) {
						return false;
					}
					if (!jobParameters.getType().isEmpty() && !jobParameters.getType().contains(job.getType())) {
						return false;
					}
					for (Pair<Set<String>, Function<HibJob, String>> prop : props) {
						if (!prop.getLeft().isEmpty() && !prop.getLeft().contains(prop.getRight().apply(job))) {
							return false;
						}
					}
					return true;
				};
			}
			Page<? extends HibJob> page = root.findAllNoPerm(ac, pagingInfo, filter);

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
