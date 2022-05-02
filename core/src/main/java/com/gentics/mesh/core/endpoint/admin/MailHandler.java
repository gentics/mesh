package com.gentics.mesh.core.endpoint.admin;

import static com.gentics.mesh.core.data.perm.InternalPermission.DELETE_PERM;
import static com.gentics.mesh.core.data.perm.InternalPermission.READ_PUBLISHED_PERM;
import static com.gentics.mesh.core.rest.error.Errors.error;
import static com.gentics.mesh.rest.Messages.message;
import static io.netty.handler.codec.http.HttpResponseStatus.*;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.sql.Timestamp;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;
import javax.inject.Singleton;

import com.gentics.mesh.cli.BootstrapInitializer;
import com.gentics.mesh.context.InternalActionContext;
import com.gentics.mesh.context.impl.DummyBulkActionContext;
import com.gentics.mesh.context.impl.InternalRoutingActionContextImpl;
import com.gentics.mesh.core.action.JobDAOActions;
import com.gentics.mesh.core.data.HibNodeFieldContainer;
import com.gentics.mesh.core.data.binary.HibBinary;
import com.gentics.mesh.core.data.branch.HibBranch;
import com.gentics.mesh.core.data.dao.BinaryDao;
import com.gentics.mesh.core.data.dao.JobDao;
import com.gentics.mesh.core.data.dao.ProjectDao;
import com.gentics.mesh.core.data.job.HibJob;
import com.gentics.mesh.core.data.node.HibNode;
import com.gentics.mesh.core.data.node.field.HibBinaryField;
import com.gentics.mesh.core.data.page.PageTransformer;
import com.gentics.mesh.core.data.project.HibProject;
import com.gentics.mesh.core.data.s3binary.S3HibBinaryField;
import com.gentics.mesh.core.data.user.HibUser;
import com.gentics.mesh.core.db.Database;
import com.gentics.mesh.core.db.Supplier;
import com.gentics.mesh.core.db.Tx;
import com.gentics.mesh.core.endpoint.handler.AbstractCrudHandler;
import com.gentics.mesh.core.rest.MeshEvent;
import com.gentics.mesh.core.rest.admin.mail.MailAttachmentsRequest;
import com.gentics.mesh.core.rest.admin.mail.MailSendingRequest;
import com.gentics.mesh.core.rest.job.JobResponse;
import com.gentics.mesh.core.rest.job.JobStatus;
import com.gentics.mesh.core.rest.schema.BinaryFieldSchema;
import com.gentics.mesh.core.rest.schema.FieldSchema;
import com.gentics.mesh.core.rest.schema.S3BinaryFieldSchema;
import com.gentics.mesh.core.verticle.handler.HandlerUtilities;
import com.gentics.mesh.core.verticle.handler.WriteLock;
import com.gentics.mesh.etc.config.MailOptions;
import com.gentics.mesh.etc.config.MeshOptions;
import com.gentics.mesh.json.JsonUtil;
import com.gentics.mesh.util.RxUtil;
import io.reactivex.Completable;
import io.reactivex.Flowable;
import io.reactivex.Observable;
import io.reactivex.Single;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.http.HttpServerResponse;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import io.vertx.ext.mail.MailAttachment;
import io.vertx.ext.mail.MailConfig;
import io.vertx.ext.mail.MailMessage;
import io.vertx.ext.mail.MailResult;
import io.vertx.ext.mail.StartTLSOptions;
import io.vertx.reactivex.core.Vertx;
import io.vertx.reactivex.core.file.FileSystem;
import io.vertx.reactivex.ext.mail.MailClient;

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
				Tx.get().jobDao().enqueueMailSending(user,new Timestamp(System.currentTimeMillis()).getTime() + meshOptions.getMailOptions().getRetry(), ac.getBodyAsString());
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
