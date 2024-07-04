package com.gentics.mesh.plugin.comment.service;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.gentics.mesh.parameter.client.NodeParametersImpl;
import com.gentics.mesh.plugin.comment.config.Configuration;
import com.gentics.mesh.plugin.comment.dagger.AdminClient;
import com.gentics.mesh.plugin.comment.model.Comment;
import com.gentics.mesh.plugin.comment.model.EffectiveCommentSection;
import com.gentics.mesh.rest.client.MeshRestClient;
import io.reactivex.Completable;
import io.vertx.core.json.JsonObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import io.vertx.ext.mail.MailMessage;
import io.vertx.reactivex.ext.mail.MailClient;
import io.vertx.reactivex.ext.web.RoutingContext;
import io.vertx.reactivex.ext.web.common.template.TemplateEngine;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.io.File;
import java.nio.file.Paths;
import java.util.List;

import static com.gentics.mesh.plugin.comment.service.MailService.Template.COMMENT_CREATED;

@Singleton
public class MailService {

	private final MailClient mailClient;
	private final TemplateEngine engine;
	private final RoutingContext context;
	private final Configuration config;
	private final MeshRestClient adminClient;
	private final String projectName;
	private final String storageDir;

	private static final Logger log = LoggerFactory.getLogger(MailService.class);

	@Inject
	public MailService(MailClient mailClient,
					   TemplateEngine engine,
					   RoutingContext context,
					   Configuration config,
					   @AdminClient MeshRestClient adminClient,
					   JsonObject project,
					   File storageDir) {
		this.mailClient = mailClient;
		this.engine = engine;
		this.context = context;
		this.config = config;
		this.adminClient = adminClient;
		this.projectName = project.getString("name");
		this.storageDir = storageDir.getPath();
	}

	public Completable notifyCommentCreated(Comment comment, EffectiveCommentSection section) {
		context.put("comment", new JsonObject(comment.getDelegate().toJson()));
		return adminClient.findNodeByUuid(
			projectName,
			section.getNode(),
			new NodeParametersImpl().setResolveLinks(this.config.getMailResolveLinks())
		).toSingle().flatMapCompletable(referencedNode -> {
			context.put("reference", new JsonObject(referencedNode.toJson()));
			return sendMail(COMMENT_CREATED, section);
		});
	}

	private Completable sendMail(Template template, EffectiveCommentSection section) {
		List<String> recipients = section.getModeratorEmails();

		if (recipients.isEmpty()) {
			return Completable.complete();
		}

		String templatePath = Paths.get(storageDir, config.getMailTemplateFolder()).toString();

		return engine.rxRender(context.data(), templatePath + template.getTemplatePath())
			.flatMap(messageBody -> {
				MailMessage message = new MailMessage();
				message.setFrom(config.getMailSender());
				message.setTo(recipients);
				message.setHtml(messageBody.toString());
				message.setSubject(config.getMailSubjects().get(template));
				return mailClient.rxSendMail(message);
			}).ignoreElement();
	}

	public enum Template {
		@JsonProperty("commentCreated")
		COMMENT_CREATED("commentCreated", "A comment has been created");

		private final String templateName;
		// TODO i18n / configurable
		private final String subject;

		Template(String templateName, String subject) {
			this.templateName = templateName;
			this.subject = subject;
		}

		public String getTemplateName() {
			return templateName;
		}

		public String getTemplatePath() {
			return "/" + templateName;
		}

		public String getSubject() {
			return subject;
		}
	}
}
