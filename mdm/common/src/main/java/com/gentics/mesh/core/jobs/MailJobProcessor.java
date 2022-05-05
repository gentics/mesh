package com.gentics.mesh.core.jobs;

import static com.gentics.mesh.core.rest.MeshEvent.MAIL_SENDING_FINISHED;
import static com.gentics.mesh.core.rest.MeshEvent.PROJECT_VERSION_PURGE_FINISHED;
import static com.gentics.mesh.core.rest.error.Errors.error;
import static com.gentics.mesh.core.rest.job.JobStatus.COMPLETED;
import static com.gentics.mesh.core.rest.job.JobStatus.FAILED;
import static io.netty.handler.codec.http.HttpResponseStatus.BAD_REQUEST;
import static io.netty.handler.codec.http.HttpResponseStatus.NOT_FOUND;

import java.util.ArrayList;

import javax.inject.Inject;

import com.gentics.mesh.context.MailSendingContext;
import com.gentics.mesh.context.impl.MailSendingContextImpl;
import com.gentics.mesh.core.data.HibNodeFieldContainer;
import com.gentics.mesh.core.data.binary.HibBinary;
import com.gentics.mesh.core.data.branch.HibBranch;
import com.gentics.mesh.core.data.dao.BinaryDao;
import com.gentics.mesh.core.data.dao.JobDao;
import com.gentics.mesh.core.data.dao.PersistingJobDao;
import com.gentics.mesh.core.data.job.HibJob;
import com.gentics.mesh.core.data.job.HibMailJob;
import com.gentics.mesh.core.data.node.HibNode;
import com.gentics.mesh.core.data.node.field.HibBinaryField;
import com.gentics.mesh.core.data.project.HibProject;
import com.gentics.mesh.core.db.Database;
import com.gentics.mesh.core.migration.impl.MigrationStatusHandlerImpl;
import com.gentics.mesh.core.rest.MeshEvent;
import com.gentics.mesh.core.rest.admin.mail.MailAttachmentsResponse;
import com.gentics.mesh.core.rest.admin.mail.MailSendingResponse;
import com.gentics.mesh.core.rest.event.job.MailSendingEventModel;
import com.gentics.mesh.core.rest.event.job.ProjectVersionPurgeEventModel;
import com.gentics.mesh.core.rest.job.JobStatus;
import com.gentics.mesh.core.rest.schema.BinaryFieldSchema;
import com.gentics.mesh.core.rest.schema.FieldSchema;
import com.gentics.mesh.etc.config.MailOptions;
import com.gentics.mesh.etc.config.MeshOptions;
import com.gentics.mesh.json.JsonUtil;
import com.gentics.mesh.parameter.impl.NodeParametersImpl;
import com.gentics.mesh.parameter.impl.VersioningParametersImpl;
import com.gentics.mesh.util.RxUtil;
import io.reactivex.Completable;
import io.reactivex.Observable;
import io.reactivex.Single;
import io.reactivex.functions.Consumer;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import io.vertx.ext.mail.MailAttachment;
import io.vertx.ext.mail.MailConfig;
import io.vertx.ext.mail.MailMessage;
import io.vertx.reactivex.core.Vertx;
import io.vertx.reactivex.ext.mail.MailClient;

/**
 * This class is responsible for starting a mail sending from a job
 */
public class MailJobProcessor implements SingleJobProcessor {

	public static final Logger log = LoggerFactory.getLogger(MailJobProcessor.class);
	private final Database db;
	private final PersistingJobDao jobDao;
	private final Vertx vertx;
	private final MeshOptions meshOptions;

	@Inject
	public MailJobProcessor(Database db, JobDao jobDao, Vertx vertx, MeshOptions meshOptions) {
		this.db = db;
		this.jobDao = (PersistingJobDao) jobDao;
		this.vertx = vertx;
		this.meshOptions = meshOptions;

	}

	@Override
	public Completable process(HibJob job) {
		HibMailJob mailJob = (HibMailJob) job;
		String mailJobId = mailJob.getUuid();
		MailConfig config = getConfig(meshOptions.getMailOptions());
		MailClient mailClient = MailClient.create(vertx, config);
		String mail = db.tx(mailJob::getMail);

		MailSendingResponse request = JsonUtil.readValue(mail, MailSendingResponse.class);
		boolean attachFiles = false;
		if(request.getAttachments()!= null && !request.getAttachments().isEmpty()){
			attachFiles = true;
		}

		MailMessage mailMessage = getMailMessage(request);
		Consumer<MailMessage> send = m -> mailClient.sendMail(m, sendRes -> {
			if (sendRes.succeeded()) {
				db.tx(tx -> {
					job.setStopTimestamp();
					job.setStatus(COMPLETED);
					jobDao.mergeIntoPersisted(job);
				});
				db.tx(tx -> {
					log.info("Job for sending the email {" + mailJobId + "} completed.");
					tx.createBatch().add(createEvent(MAIL_SENDING_FINISHED, COMPLETED))
						.dispatch();
				});
			} else {
				markJobAsFailed(mailJobId, new Throwable(sendRes.cause()));
				db.tx(tx -> {
					log.info("Job for sending the email {" + mailJob.getUuid() + "} completed.");
					tx.createBatch().add(createEvent(MAIL_SENDING_FINISHED, FAILED))
						.dispatch();
				});
				log.error("Error while sending mail", sendRes.cause());
			}
		});
		if(mail != null) {
			if (attachFiles) {
				addAttachments(request.getAttachments(), mailMessage, mailJobId).subscribe(send);
			} else {
				Single.just(mailMessage).subscribe(send);
			}
		} else {
			markJobAsFailed(mailJobId, new IllegalArgumentException("There is no message provided"));
			db.tx(tx -> {
				log.info("Job for sending the email {" + mailJob.getUuid() + "} completed.");
				tx.createBatch().add(createEvent(MAIL_SENDING_FINISHED, FAILED))
					.dispatch();
			});
			return Completable.complete();
		}
		return Completable.complete();
	}

	/**
	 * Mark the job as failed
	 * @param mailJobId
	 * @param sendRes
	 */
	private void markJobAsFailed(String mailJobId, Throwable sendRes) {
		db.tx(tx -> {
			HibJob mJob = tx.jobDao().findByUuid(mailJobId);
			mJob.setStopTimestamp();
			mJob.setStatus(FAILED);
			mJob.setError(sendRes);
			jobDao.mergeIntoPersisted(mJob);
		});
	}

	/**
	 * The custom context of the job will be filled with the data being retrieved from the database
	 * @param jobId
	 * @param binaryField
	 * @return
	 */
	private MailSendingContext prepareContext(String jobId, MailAttachmentsResponse binaryField) {
		MigrationStatusHandlerImpl status = new MigrationStatusHandlerImpl(jobId);
		try {
			return db.tx(tx -> {
				MailSendingContextImpl context = new MailSendingContextImpl();
				HibProject project = tx.projectDao().findByName(binaryField.getProject());
				if (project == null) {
					throw error(NOT_FOUND, "mail_sending_project_not_found",binaryField.getProject());
				}
				HibBranch branch = null;
				if(binaryField.getBranch() != null && !!binaryField.getBranch().isEmpty()) {
					branch = tx.branchDao().findByName(project, binaryField.getBranch());
				}
				if(branch == null){
					log.info("The branch was not found, continue within the default branch");
					branch = project.getLatestBranch();
				}

				HibNode node = tx.nodeDao().findByUuid(project, binaryField.getUuid());
				if (node == null) {
					throw error(NOT_FOUND, "mail_sending_node_not_found",binaryField.getUuid());
				}
				BinaryDao binaryDao = tx.binaryDao();
				context.setBinaryDao(binaryDao);
				context.setProject(project);
				context.setStatus(status);
				context.setBranch(branch);
				context.setHibNode(node);
				HibNodeFieldContainer fieldContainer = tx.contentDao()
					.findVersion(node, new NodeParametersImpl(context).getLanguageList(meshOptions),
					branch.getUuid(),
					new VersioningParametersImpl(context).getVersion());
				if (fieldContainer == null) {
					throw error(NOT_FOUND, "object_not_found_for_version",context.getVersioningParameters().getVersion());
				}
				FieldSchema fieldSchema = fieldContainer.getSchemaContainerVersion().getSchema().getField(binaryField.getField());
				if (fieldSchema == null) {
					throw error(BAD_REQUEST, "error_schema_definition_not_found", binaryField.getField());
				}
				if ((fieldSchema instanceof BinaryFieldSchema)) {
					HibBinaryField field = fieldContainer.getBinary(binaryField.getField());
					if (field == null) {
						throw error(NOT_FOUND, "error_binaryfield_not_found_with_name", binaryField.getField());
					}
					HibBinary binary = field.getBinary();
					context.setHibBinary(binary);
					context.setBinaryMimeType(field.getMimeType());
					context.setBinaryName(field.getDisplayName());
				}
				return context;
			});
		} catch (Exception e) {
			throw e;
		}

	}

	/**
	 * Create a mail config object out of the configs coming from mesh.yml
	 * @param mailOptions
	 * @return MailConfig object
	 */
	private MailConfig getConfig(MailOptions mailOptions) {
		MailConfig config = new MailConfig();
		config.setHostname(mailOptions.getHostname());
		config.setPort(mailOptions.getPort());
		config.setStarttls(mailOptions.getStartTls());
		config.setUsername(mailOptions.getUsername());
		config.setPassword(mailOptions.getPassword());
		config.setSsl(mailOptions.isSsl());
		config.setTrustAll(mailOptions.isTrustAll());
		config.setMaxPoolSize(mailOptions.getMaxPoolSize());
		config.setKeepAlive(mailOptions.getIskeepAlive());
		return config;
	}

	/**
	 * Create a mail message Object out of the data coming from the request
	 * @param request
	 * @return MailMessage object
	 */
	private MailMessage getMailMessage(MailSendingResponse request) {
		MailMessage message = new MailMessage();
		message.setFrom(request.getFrom());
		message.setSubject(request.getSubject());
		message.setTo(request.getTo());
		message.setCc(request.getCc());
		message.setBcc(request.getBcc());
		message.setText(request.getText());
		message.setHtml(request.getHtml());
		return message;
	}

	/**
	 * Add files from binary fields as attachments to message
	 *
	 * @return single message
	 */
	private Single<MailMessage> addAttachments(ArrayList<MailAttachmentsResponse> attachments, MailMessage message, String jobId) {
		message.setAttachment(new ArrayList<>());
		return Observable.fromIterable(attachments)
			.filter(it-> !it.getProject().isEmpty() && !it.getUuid().isEmpty() || !it.getField().isEmpty())
			.flatMapSingle(it-> {
				MailSendingContext context = prepareContext(jobId, it);
				BinaryDao binaryDao = context.getBinaryDao();
				HibBinary binary = context.getHibBinary();
				return db.tx(tx -> {
					return RxUtil.readEntireData(binaryDao.getStream(binary))
						.map(buffer -> {
							message.getAttachment()
								.add(new MailAttachment().setName(context.getBinaryName()).setContentType(context.getBinaryMimeType()).setData(buffer));
							return message;
						});
				});
			})
			.lastOrError()
			.doOnError(th-> {
				markJobAsFailed(jobId, th);
				db.tx(tx -> {
						tx.createBatch().add(createEvent(MAIL_SENDING_FINISHED, FAILED))
							.dispatch();
					});
				log.error("The attachment couldn't be included", th.getMessage());
			});


	}

	/**
	 *
	 * @param event
	 * @param status
	 * @return
	 */
	private MailSendingEventModel createEvent(MeshEvent event, JobStatus status) {
		MailSendingEventModel model = new MailSendingEventModel();
		model.setEvent(event);
		model.setStatus(status);
		return model;
	}

}
