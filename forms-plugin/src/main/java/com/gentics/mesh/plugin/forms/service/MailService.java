package com.gentics.mesh.plugin.forms.service;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import javax.inject.Inject;
import javax.inject.Singleton;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.gentics.mesh.core.rest.node.NodeResponse;
import com.gentics.mesh.core.rest.node.field.BinaryField;
import com.gentics.mesh.core.rest.node.field.impl.StringFieldImpl;
import com.gentics.mesh.plugin.PluginContext;
import com.gentics.mesh.plugin.common.TemplateWrapper;
import com.gentics.mesh.plugin.common.dagger.AdminClient;
import com.gentics.mesh.plugin.common.service.RenderService;
import com.gentics.mesh.plugin.common.util.ParameterUtil;
import com.gentics.mesh.plugin.forms.config.FormsConfig;
import com.gentics.mesh.plugin.forms.config.MailOptions;
import com.gentics.mesh.plugin.forms.model.AdminMailElement;
import com.gentics.mesh.plugin.forms.model.BinaryElement;
import com.gentics.mesh.plugin.forms.model.Form;
import com.gentics.mesh.plugin.forms.model.FormElement;
import com.gentics.mesh.plugin.forms.model.UsermailElement;
import com.gentics.mesh.plugin.forms.util.RxUtil;
import com.gentics.mesh.rest.client.MeshRestClient;

import io.reactivex.Observable;
import io.reactivex.Single;
import io.reactivex.functions.Consumer;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.mail.MailAttachment;
import io.vertx.ext.mail.MailClient;
import io.vertx.ext.mail.MailConfig;
import io.vertx.ext.mail.MailMessage;

@Singleton
public class MailService {
	private static final Logger log = LoggerFactory.getLogger(MailService.class);
	private static final String PLACEHOLDER_FORMNAME = "[[FORMNAME]]";

	private final MeshRestClient adminClient;

	private final Vertx vertx;

	private final FormsConfig configuration;

	private final RenderService renderService;

	@Inject
	public MailService(
			@AdminClient MeshRestClient adminClient,
			Vertx vertx,
			FormsConfig configuration,
			RenderService renderService) {
		this.adminClient = adminClient;
		this.vertx = vertx;
		this.configuration = configuration;
		this.renderService = renderService;
	}

	/**
	 * Send admin and user mails for a submitted form. The language for label resolution is extracted
	 * from the {@code lang} query parameter of the given context.
	 *
	 * @param projectName  Mesh project name
	 * @param form         the submitted form
	 * @param formDataNode the saved form data node
	 * @param context      plugin context
	 */
	public void sendMails(String projectName, Form form, NodeResponse formDataNode, PluginContext context) {
		ParameterUtil.getFirstLanguage(context, null).subscribe(language -> {
			boolean attachFileUploads = configuration.getMailOptions().isAttachFileUploads();
			// Check for admin mails in AdminMailElements.
			List<String> emails = form.getElementsInsideProperties()
				.filter(e -> e instanceof AdminMailElement)
				.map(e -> ((AdminMailElement) e).getSelectedEmails(formDataNode, language))
				.flatMap(List::stream)
				.collect(Collectors.toList());

			// Fallback to admin mail configured in the form itself.
			if (emails.isEmpty()) {
				String configuredAdminMail = StringUtils.trim(form.getEmail());

				emails = StringUtils.isBlank(configuredAdminMail)
					? Collections.emptyList()
					: Arrays.asList(StringUtils.split(configuredAdminMail, ','));
			}

			// send mail to admins
			if (!emails.isEmpty()) {
				TemplateWrapper template = TemplateWrapper.wrap(form.getMailsource(), form.getMailtemp(), configuration.getAdminMailTemplate());

				if (template.isValid()) {
					String subject = StringUtils.defaultIfBlank(form.getMailsubject(), configuration.getAdminMailSubject());

					subject = StringUtils.defaultIfBlank(subject, PLACEHOLDER_FORMNAME)
						.replace(PLACEHOLDER_FORMNAME, form.getName());

					sendMail(projectName, form, formDataNode, context, subject, template, emails, attachFileUploads, language);
				} else {
					log.error(
							"Could not send admin mail for form {}, because no template configuration was found (template must be either configured in config.yml as \"adminMailTemplate\" or set in the form as \"mailtemp\").",
							form.getName());
				}
			}

			form.getElementsInsideProperties().filter(e -> e instanceof UsermailElement).map(e -> (UsermailElement) e).forEach(element -> {
				StringFieldImpl field = formDataNode.getFields().getStringField(element.getName());
				if (field == null) {
					return;
				}
				String userEmail = field.getString();
				if (StringUtils.isBlank(userEmail)) {
					return;
				}

				TemplateWrapper template = TemplateWrapper.wrap(element.getMailsource(), element.getTemplate(), configuration.getUserMailTemplate());

				if (!template.isValid()) {
					log.error(
							"Could not send user mail for element {} in form {}, because no template configuration was found (template must be either configured in config.yml as \"userMailTemplate\" or set in the element as \"mailtemplate\").",
							element.getLabelOrName(), form.getName());
					return;
				}
				String subject = StringUtils.defaultIfBlank(element.getSubject(), configuration.getUserMailSubject());

				subject = StringUtils.defaultIfBlank(subject, PLACEHOLDER_FORMNAME)
					.replace(PLACEHOLDER_FORMNAME, form.getName());

				sendMail(projectName, form, formDataNode, context, subject, template, Collections.singletonList(userEmail), attachFileUploads, language);
			});
		}, error -> log.error("Error extracting language for mail sending", error));
	}

	/**
	 * Send a mail asynchronously. Option labels in the rendered template are resolved using
	 * the given language.
	 *
	 * @param projectName  the Mesh project name
	 * @param form         the form
	 * @param formDataNode the form data node
	 * @param context      the plugin context
	 * @param subject      the mail subject
	 * @param template     the mail template
	 * @param to           recipient address(es)
	 * @param attachFiles  whether to attach binary form fields as file attachments
	 * @param language     BCP-47 language tag for label resolution, may be {@code null}
	 */
	private void sendMail(String projectName, Form form, NodeResponse formDataNode, PluginContext context, String subject, TemplateWrapper template, List<String> to,
			boolean attachFiles, String language) {
		String from = configuration.getFromAddress();
		if (StringUtils.isBlank(from)) {
			log.warn("Sending mail will probably fail, because no \"fromAddress\" is configured in config.yml.");
		}

		context.put("baseUrl", FormDataService.getDownloadBaseUrl(form, projectName));
		context.put("form", new JsonObject(form.removeInactiveElements().toJson()));
		context.put("data", form.getReportableFormData(formDataNode, e -> e.isActive(), language));
		context.put("dataUuid", formDataNode.getUuid());

		renderService.render(context.data(), template, res -> {
			if (res.succeeded()) {
				MailMessage message = new MailMessage().setTo(to).setFrom(from).setSubject(subject).setHtml(res.result().toString());

				Consumer<MailMessage> send = m -> {
					MailClient client = MailClient.createShared(vertx, convertConfig(configuration.getMailOptions()), "client");
					client.sendMail(m).andThen(sendRes -> {
						if (sendRes.succeeded()) {
							log.debug("Main sent: " + sendRes.result().toString());
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
	 * @param mailOptions The plugin mail options.
	 * @return Vert.x mail configuration.
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
		List<String> fileFieldNames = form.getElementsInsideProperties()
			.filter(element -> element instanceof BinaryElement)
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
