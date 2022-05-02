package com.gentics.mesh.core.jobs;

import static com.gentics.mesh.core.rest.error.Errors.error;
import static com.gentics.mesh.core.rest.job.JobStatus.*;
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
import com.gentics.mesh.core.data.dao.NodeDao;
import com.gentics.mesh.core.data.dao.PersistingJobDao;
import com.gentics.mesh.core.data.job.HibJob;
import com.gentics.mesh.core.data.job.HibMailJob;
import com.gentics.mesh.core.data.node.HibNode;
import com.gentics.mesh.core.data.node.field.HibBinaryField;
import com.gentics.mesh.core.data.project.HibProject;
import com.gentics.mesh.core.db.Database;
import com.gentics.mesh.core.db.Tx;
import com.gentics.mesh.core.endpoint.migration.MigrationStatusHandler;
import com.gentics.mesh.core.migration.impl.MigrationStatusHandlerImpl;
import com.gentics.mesh.core.rest.admin.mail.MailAttachmentsRequest;
import com.gentics.mesh.core.rest.admin.mail.MailSendingRequest;
import com.gentics.mesh.core.rest.event.node.BranchMigrationCause;
import com.gentics.mesh.core.rest.event.node.MailSendingCause;
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
import io.reactivex.SingleSource;
import io.reactivex.functions.Consumer;
import io.reactivex.functions.Function;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import io.vertx.ext.mail.MailAttachment;
import io.vertx.ext.mail.MailConfig;
import io.vertx.ext.mail.MailMessage;
import io.vertx.ext.mail.StartTLSOptions;
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
		MailConfig config = getConfig(meshOptions.getMailOptions());
		MailClient mailClient = MailClient.create(vertx, config);
		String mail = db.tx(mailJob::getMail);

		MailSendingRequest request = JsonUtil.readValue(mail, MailSendingRequest.class);
		boolean attachFiles = false;
		if(request.getAttachments()!= null && !request.getAttachments().isEmpty()){
			attachFiles = true;
		}

		MailMessage mailMessage = getMailMessage(request);
		Consumer<MailMessage> send = m -> mailClient.sendMail(m, sendRes -> {
			if (sendRes.succeeded()) {
				db.tx(() -> {
					mailJob.setStopTimestamp();
					mailJob.setStatus(COMPLETED);
					jobDao.mergeIntoPersisted(mailJob);
				});
			} else {
				db.tx(() -> {
						markJobAsFailed(mailJob, (Throwable) sendRes);
					});
				log.error("Error while sending mail", sendRes.cause());
			}
		});
		if(mail != null) {
			if (attachFiles) {
				addAttachments(request.getAttachments(), mailMessage, job).subscribe(send);
			} else {
				Single.just(mailMessage).subscribe(send);
			}
		} else {
			markJobAsFailed(mailJob, new IllegalArgumentException("There is no message provided"));
			return Completable.complete();
		}
		return Completable.complete();
	}

	/**
	 * Mark the job as failed
	 * @param mailJob
	 * @param sendRes
	 */
	private void markJobAsFailed(HibMailJob mailJob, Throwable sendRes) {
			mailJob.setStopTimestamp();
			mailJob.setStatus(FAILED);
			mailJob.setError(sendRes);
			jobDao.mergeIntoPersisted(mailJob);
	}

	/**
	 * The custom context of the job will be filled with the data being retrieved from the database
	 * @param job
	 * @param binaryField
	 * @return
	 */
	private MailSendingContext prepareContext(HibJob job, MailAttachmentsRequest binaryField) {
		HibMailJob mailJob = (HibMailJob) job;
		MigrationStatusHandlerImpl status = new MigrationStatusHandlerImpl(job.getUuid());
		try {
			return db.tx(tx -> {
				MailSendingContextImpl context = new MailSendingContextImpl();
				HibProject project = tx.projectDao().findByName(binaryField.getProject());
				if (project == null) {
					markJobAsFailed(mailJob, new IllegalArgumentException("The Project could not be found"));
					throw error(NOT_FOUND, "object_not_found_for_version",binaryField.getProject());

				}
				HibBranch branch = project.getLatestBranch();
				HibNode node = tx.nodeDao().findByUuid(project, binaryField.getUuid());
				if (node == null) {
					markJobAsFailed(mailJob, new IllegalArgumentException("The Binary could not be found"));
					throw error(NOT_FOUND, "object_not_found_for_version",binaryField.getUuid());
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
					markJobAsFailed(mailJob, new IllegalArgumentException("The Binary could not be found for the version given"));
					throw error(NOT_FOUND, "object_not_found_for_version",context.getVersioningParameters().getVersion());
				}
				FieldSchema fieldSchema = fieldContainer.getSchemaContainerVersion().getSchema().getField(binaryField.getField());
				if (fieldSchema == null) {
					markJobAsFailed(mailJob, new IllegalArgumentException("The schema of the binary could not be found"));
					throw error(BAD_REQUEST, "error_schema_definition_not_found", binaryField.getField());
				}
				if ((fieldSchema instanceof BinaryFieldSchema)) {
					HibBinaryField field = fieldContainer.getBinary(binaryField.getField());
					if (field == null) {
						throw error(NOT_FOUND, "error_binaryfield_not_found_with_name", binaryField.getField());
					}

					HibBinary binary = field.getBinary();MailSendingCaus
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
	private Single<MailMessage> addAttachments(ArrayList<MailAttachmentsRequest> attachments, MailMessage message, HibJob job) {
		message.setAttachment(new ArrayList<>());
		return Observable.fromIterable(attachments)
			.filter(it-> !it.getProject().isEmpty() && !it.getUuid().isEmpty() || !it.getField().isEmpty())
			.flatMapSingle(it-> {
				MailSendingContext context = prepareContext(job, it);
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
			.lastOrError().doOnError(th-> {

			});


	}

}
