package com.gentics.mesh.plugin.formgenerator.service;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import javax.inject.Inject;
import javax.inject.Singleton;

import org.apache.commons.lang3.StringUtils;

import com.gentics.mesh.core.rest.node.NodeResponse;
import com.gentics.mesh.core.rest.node.field.BinaryField;
import com.gentics.mesh.core.rest.node.field.impl.StringFieldImpl;
import com.gentics.mesh.json.JsonUtil;
import com.gentics.mesh.plugin.common.dagger.AdminClient;
import com.gentics.mesh.plugin.formgenerator.config.FormGeneratorConfig;
import com.gentics.mesh.plugin.formgenerator.config.MailOptions;
import com.gentics.mesh.plugin.formgenerator.dagger.BaseUrls;
import com.gentics.mesh.plugin.formgenerator.model.FileElement;
import com.gentics.mesh.plugin.formgenerator.model.Form;
import com.gentics.mesh.plugin.formgenerator.model.FormElement;
import com.gentics.mesh.plugin.formgenerator.model.UsermailElement;
import com.gentics.mesh.plugin.formgenerator.util.RxUtil;
import com.gentics.mesh.rest.client.MeshRestClient;

import io.reactivex.Observable;
import io.reactivex.Single;
import io.reactivex.functions.Consumer;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import io.vertx.ext.mail.MailAttachment;
import io.vertx.ext.mail.MailClient;
import io.vertx.ext.mail.MailConfig;
import io.vertx.ext.mail.MailMessage;
import io.vertx.ext.web.RoutingContext;
import io.vertx.reactivex.ext.web.templ.handlebars.HandlebarsTemplateEngine;

@Singleton
public class MailService {
	private static final Logger log = LoggerFactory.getLogger(MailService.class);

	private final MeshRestClient adminClient;

	private final Vertx vertx;

	private final FormGeneratorConfig configuration;

	private final HandlebarsTemplateEngine templateEngine;

	private final Map<String, String> baseUrls;

	@Inject
	public MailService(
			@AdminClient MeshRestClient adminClient,
			Vertx vertx,
			FormGeneratorConfig configuration,
			HandlebarsTemplateEngine templateEngine,
			@BaseUrls Map<String, String> baseUrls) {
		this.adminClient = adminClient;
		this.vertx = vertx;
		this.configuration = configuration;
		this.templateEngine = templateEngine;
		this.baseUrls = baseUrls;
	}

	/**
	 * Send mails
	 *
	 * @param projectName project name
	 * @param form
	 *            form
	 * @param formDataNode
	 *            form data
	 * @param rc
	 *            routing context
	 */
	public void sendMails(String projectName, Form form, NodeResponse formDataNode, RoutingContext rc) {
		boolean attachFileUploads = configuration.getMailOptions().isAttachFileUploads();
		// send mail to admin
		String adminEmail = StringUtils.trim(form.getEmail());
		if (!StringUtils.isEmpty(adminEmail)) {
			List<String> emails = Arrays.asList(StringUtils.split(adminEmail, ','));

			String template = StringUtils
				.trim(StringUtils.defaultIfBlank(form.getMailtemp(), configuration.getAdminMailTemplate()));
			if (!StringUtils.isBlank(template)) {
				String subject = StringUtils.defaultIfBlank(form.getMailsubject(), configuration.getAdminMailSubject())
					.replace("[[FORMNAME]]", form.getName());

				sendMail(projectName, form, formDataNode, rc, subject, template, emails, attachFileUploads);
			}
		}

		form.flattened().filter(e -> e instanceof UsermailElement).map(e -> (UsermailElement) e).forEach(element -> {
			StringFieldImpl field = formDataNode.getFields().getStringField(element.getName());
			if (field == null) {
				return;
			}
			String userEmail = field.getString();
			if (StringUtils.isBlank(userEmail)) {
				return;
			}
			String template = StringUtils
				.trim(StringUtils.defaultIfBlank(element.getTemplate(), configuration.getUserMailTemplate()));
			if (StringUtils.isBlank(template)) {
				return;
			}
			String subject = StringUtils.defaultIfBlank(element.getSubject(), configuration.getUserMailSubject()).replace(
				"[[FORMNAME]]",
				form.getName());
			sendMail(projectName, form, formDataNode, rc, subject, template, Arrays.asList(userEmail), attachFileUploads);
		});
	}

	/**
	 * Send a mail
	 *
	 * @param projectName
	 * @param form
	 *            form
	 * @param formDataNode
	 *            form data node
	 * @param rc
	 *            routing context
	 * @param subject
	 *            mail subject
	 * @param template
	 *            mail template name
	 * @param to
	 *            recipient(s)
	 * @param attachFiles
	 *            flag for attaching binary fields as files
	 */
	private void sendMail(String projectName, Form form, NodeResponse formDataNode, RoutingContext rc, String subject, String template, List<String> to,
			boolean attachFiles) {
		String from = configuration.getFromAddress();

		rc.put("baseUrl", baseUrls.get(projectName));
		rc.put("form", new JsonObject(JsonUtil.toJson(form)));
		rc.put("data", form.getReportableFormData(formDataNode));
		rc.put("dataUuid", formDataNode.getUuid());
		templateEngine.render(rc.data(), template, res -> {
			if (res.succeeded()) {
				MailMessage message = new MailMessage().setTo(to).setFrom(from).setSubject(subject).setHtml(res.result().toString());

				Consumer<MailMessage> send = m -> {
					MailClient client = MailClient.createShared(vertx, convertConfig(configuration.getMailOptions()), "client");
					client.sendMail(m, sendRes -> {
						if (sendRes.succeeded()) {
							log.info(sendRes.result());
						} else {
							log.error("Error while sending mail", sendRes.cause());
						}
					});
				};
				if (attachFiles) {
					addAttachments(projectName, form, formDataNode, message).subscribe(send);
				} else {
					Single.just(message).subscribe(send);
				}
			} else {
				log.error("Error while rendering mail body", res.cause());
			}
		});
	}

	/**
	 * Transform the options to Vert.x configuration.
	 *
	 * @param mailOptions
	 * @return Vert.x mail configuration
	 */
	private MailConfig convertConfig(MailOptions mailOptions) {
		MailConfig config = new MailConfig();
		config.setHostname(mailOptions.getHostname());
		config.setPort(mailOptions.getPort());
		config.setAuthMethods(mailOptions.getAuthMethods());
		config.setUsername(mailOptions.getUsername());
		config.setPassword(mailOptions.getPassword());
		config.setSsl(mailOptions.isSsl());
		config.setTrustAll(mailOptions.isTrustAll());
		config.setStarttls(mailOptions.getStartTls());
		config.setOwnHostname(mailOptions.getOwnHostname());
		config.setMaxPoolSize(mailOptions.getMaxPoolSize());
		config.setKeepAlive(mailOptions.isKeepAlive());
		return config;
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
	private Single<MailMessage> addAttachments(String projectName, Form form, NodeResponse formDataNode, MailMessage message) {
		List<String> fileFieldNames = form.flattened()
			.filter(element -> element instanceof FileElement)
			.map(FormElement::getName)
			.filter(name -> {
				BinaryField field = formDataNode.getFields().getBinaryField(name);
				return (field != null && field.hasValues());
			})
			.collect(Collectors.toList());

		if (fileFieldNames.isEmpty()) {
			return Single.just(message);
		}

		// prepare attachments
		message.setAttachment(new ArrayList<>());

		// download all binary data and add as attachments
		return Observable.fromIterable(fileFieldNames).flatMapSingle(name -> {
			return adminClient.downloadBinaryField(projectName, formDataNode.getUuid(), formDataNode.getLanguage(), name).toSingle();
		}).flatMapSingle(download -> RxUtil.readEntireData(download.getFlowable())
		.map(buffer -> {
			message.getAttachment()
				.add(MailAttachment.create().setName(download.getFilename()).setContentType(download.getContentType()).setData(buffer));
			return message;
		})).lastOrError();
	}
}
