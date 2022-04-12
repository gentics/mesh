package com.gentics.mesh.core.endpoint.admin;

import static com.gentics.mesh.core.rest.error.Errors.error;
import static com.gentics.mesh.rest.Messages.message;
import static io.netty.handler.codec.http.HttpResponseStatus.*;

import java.util.ArrayList;

import javax.inject.Inject;
import javax.inject.Singleton;

import com.gentics.mesh.cli.BootstrapInitializer;
import com.gentics.mesh.context.InternalActionContext;
import com.gentics.mesh.core.action.JobDAOActions;
import com.gentics.mesh.core.data.job.HibJob;
import com.gentics.mesh.core.data.page.PageTransformer;
import com.gentics.mesh.core.db.Database;
import com.gentics.mesh.core.endpoint.handler.AbstractCrudHandler;
import com.gentics.mesh.core.rest.MeshEvent;
import com.gentics.mesh.core.rest.admin.mail.MailAttachmentsRequest;
import com.gentics.mesh.core.rest.admin.mail.MailSendingRequest;
import com.gentics.mesh.core.rest.job.JobResponse;
import com.gentics.mesh.core.verticle.handler.HandlerUtilities;
import com.gentics.mesh.core.verticle.handler.WriteLock;
import com.gentics.mesh.etc.config.MailOptions;
import com.gentics.mesh.etc.config.MeshOptions;
import com.gentics.mesh.json.JsonUtil;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import io.vertx.ext.mail.MailConfig;
import io.vertx.ext.mail.MailMessage;
import io.vertx.ext.mail.StartTLSOptions;
import io.vertx.reactivex.core.Vertx;
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
			MailSendingRequest request = JsonUtil.readValue(ac.getBodyAsString(), MailSendingRequest.class);
			MailMessage message = getMailMessage(request);

			MailConfig config = getConfig(meshOptions.getMailOptions());
			MailClient mailClient = MailClient.create(vertx, config);
			mailClient.rxSendMail(message)
				.subscribe(System.out::println, throwable -> {
					throwable.printStackTrace();
				});
			MeshEvent.triggerJobWorker(boot.mesh());
			return message(ac, "mail_sending_initiated");
		}, model -> ac.send(model, OK));
	}

	private MailConfig getConfig(MailOptions mailOptions) {
		MailConfig config = new MailConfig();
		config.setHostname(mailOptions.getHostname());
		config.setPort(mailOptions.getPort());
		config.setStarttls(StartTLSOptions.OPTIONAL);
		config.setUsername(mailOptions.getUsername());
		config.setPassword(mailOptions.getPassword());
		config.setSsl(mailOptions.isSsl());
		config.setTrustAll(mailOptions.isTrustAll());
		//config.setStarttls(mailOptions.getStartTls());
		config.setOwnHostname(mailOptions.getOwnHostname());
		config.setMaxPoolSize(mailOptions.getMaxPoolSize());
		config.setKeepAlive(mailOptions.getIskeepAlive());
		return config;
	}

	private MailMessage getMailMessage(MailSendingRequest request) {
		MailMessage message = new MailMessage();
		message.setFrom(request.getFrom());
		message.setTo(request.getTo());
		message.setCc(request.getCc());
		message.setBcc(request.getBcc());
		message.setText(request.getText());
		message.setHtml(request.getHtml());
		ArrayList<MailAttachmentsRequest> attachments = request.getAttachments();
		//message.setAttachment((MailAttachment) request.getAttachments());
		return message;
	}

	/**
	 * Add files from binary fields as attachments to message
	 *
	 * @param projectName project name
	 * @param form
	 *            form
	 * @param formDataNode
	 *            form data
	 * @param message
	 *            message
	 * @return single message
	 */
	/*private MailMessage addAttachments(ArrayList<MailAttachmentsRequest> attachments, MailMessage message) {

		attachments.forEach(attachment -> {
				if (attachment.getProject().isEmpty() || attachment.getUuid().isEmpty() || attachment.getField().isEmpty()) {
					return;
				} else {

				}
			});

		// prepare attachments
		message.setAttachment(new ArrayList<>());

		// download all binary data and add as attachments
		return Observable.fromIterable(fileFieldNames).flatMapSingle(name -> {
			return adminClient.downloadBinaryField(projectName, formDataNode.getUuid(), formDataNode.getLanguage(), name).toSingle();
		}).flatMapSingle(download -> RxUtil.readEntireData(download.getFlowable())
			.map(buffer -> {
				message.getAttachment()
					.add(new MailAttachment().setName(download.getFilename()).setContentType(download.getContentType()).setData(buffer));
				return message;
			})).lastOrError();
	}*/
}
