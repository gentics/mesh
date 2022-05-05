package com.gentics.mesh.core.endpoint.admin;

import static com.gentics.mesh.core.rest.error.Errors.error;
import static com.gentics.mesh.rest.Messages.message;
import static io.netty.handler.codec.http.HttpResponseStatus.*;

import java.sql.Timestamp;

import javax.inject.Inject;
import javax.inject.Singleton;

import com.gentics.mesh.cli.BootstrapInitializer;
import com.gentics.mesh.context.InternalActionContext;
import com.gentics.mesh.context.impl.DummyBulkActionContext;
import com.gentics.mesh.core.action.JobDAOActions;
import com.gentics.mesh.core.data.job.HibJob;
import com.gentics.mesh.core.data.page.PageTransformer;
import com.gentics.mesh.core.data.user.HibUser;
import com.gentics.mesh.core.db.Database;
import com.gentics.mesh.core.db.Tx;
import com.gentics.mesh.core.endpoint.handler.AbstractCrudHandler;
import com.gentics.mesh.core.rest.MeshEvent;
import com.gentics.mesh.core.rest.job.JobResponse;
import com.gentics.mesh.core.rest.job.JobStatus;
import com.gentics.mesh.core.verticle.handler.HandlerUtilities;
import com.gentics.mesh.core.verticle.handler.WriteLock;
import com.gentics.mesh.etc.config.MeshOptions;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import io.vertx.reactivex.core.Vertx;

/**
 * REST handler for job endpoint operations.
 */
@Singleton
public class MailHandler extends AbstractCrudHandler<HibJob, JobResponse> {

	private static final Logger log = LoggerFactory.getLogger(MailHandler.class);

	private BootstrapInitializer boot;

	private final PageTransformer pageTransformer;

	private final Vertx vertx;

	private final MeshOptions meshOptions;

	@Inject
	public MailHandler(Database db, BootstrapInitializer boot, HandlerUtilities utils, WriteLock writeLock, JobDAOActions jobActions,
					   PageTransformer pageTransformer, Vertx vertx, MeshOptions meshOptions) {
		super(db, utils, writeLock, jobActions);
		this.boot = boot;
		this.pageTransformer = pageTransformer;
		this.vertx = vertx;
		this.meshOptions = meshOptions;
	}


	/**
	 * Invoke the job verticle for email sending.
	 * 
	 * @param ac
	 */
	public void handleInvokeMailWorker(InternalActionContext ac) {
		utils.syncTx(ac, (tx) -> {
			if (!ac.getUser().isAdmin()) {
				throw error(FORBIDDEN, "error_admin_permission_required");
			}
			HibUser user = ac.getUser();
			HibJob mailSendingJob = Tx.get().jobDao().enqueueMailSending(user, new Timestamp(System.currentTimeMillis()).getTime(), ac.getBodyAsString());

			if(mailSendingJob.getStatus() == JobStatus.FAILED){
				mailSendingJob = Tx.get().jobDao().enqueueMailSending(user,new Timestamp(System.currentTimeMillis()).getTime() + meshOptions.getMailOptions().getRetry(), ac.getBodyAsString());
			}
			if (mailSendingJob.getStatus() == JobStatus.COMPLETED) {
				 Tx.get().jobDao().delete(mailSendingJob, new DummyBulkActionContext());
			}
			return message(ac, "mail_sending_initiated");
		}, model ->{
				MeshEvent.triggerJobWorker(boot.mesh());
				ac.send(model, OK);
			});
	}

}
