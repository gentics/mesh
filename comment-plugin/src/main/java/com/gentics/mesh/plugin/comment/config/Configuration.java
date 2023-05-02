package com.gentics.mesh.plugin.comment.config;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import com.gentics.mesh.etc.config.env.EnvironmentVariable;
import com.gentics.mesh.parameter.LinkType;
import com.gentics.mesh.plugin.comment.model.AnonymousCommentField;
import com.gentics.mesh.plugin.comment.service.MailService;
import io.vertx.ext.mail.MailConfig;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.gentics.mesh.plugin.comment.model.AnonymousCommentField.AUTHOR;
import static com.gentics.mesh.plugin.comment.model.AnonymousCommentField.EMAIL;

/**
 * Central comment plugin configuration.
 */
public class Configuration {

	public static final boolean DEFAULT_REQUIRES_APPROVAL = false;
	public static final String DEFAULT_SCHEMA_PREFIX = "comment_";
	public static final boolean DEFAULT_CACHE_TEMPLATES = true;
	public static boolean DEFAULT_ALLOW_ANONYMOUS = true;
	public static List<String> DEFAULT_MODERATOR_EMAILS = new ArrayList<>();
	public static List<AnonymousCommentField> DEFAULT_REQUIRED_FOR_ANONYMOUS = Arrays.asList(EMAIL, AUTHOR);
	public static final MailConfig DEFAULT_MAIL_CONFIG = new MailConfig();
	public static final String DEFAULT_MAIL_TEMPLATE_FOLDER = "plugins/comment/mailTemplates";
	public static final String DEFAULT_MAIL_SENDER = "Admin <admin@example.com>";
	public static final Map<MailService.Template, String> DEFAULT_MAIL_SUBJECTS = defaultMailSubjects();
	public static final String DEFAULT_NODE_CONFIG_FIELD = null;
	public static final LinkType DEFAULT_MAIL_RESOLVE_LINKS = LinkType.SHORT;
	public static final String LICENSE_KEY_ENV = "LICENSE_KEY";

	private static Map<MailService.Template, String> defaultMailSubjects() {
		HashMap<MailService.Template, String> map = new HashMap<>();
		map.put(MailService.Template.COMMENT_CREATED, "A comment has been created");
		return map;
	}

	@JsonProperty(required = true)
	@JsonPropertyDescription("Root element in which the comments data will be placed. This can either point to a webroot path or list the uuid of the node.")
	private String commentRoot;

	@JsonProperty(required = true)
	@JsonPropertyDescription("List of projects for which the comments plugin is enabled")
	private List<String> projectNames;

	@JsonProperty(defaultValue = "license.key")
	@JsonPropertyDescription("Path to the license key file.")
	@EnvironmentVariable(name = LICENSE_KEY_ENV, description = "Override the license key.")
	private String licenseKey = "license.key";

	private String schemaPrefix = DEFAULT_SCHEMA_PREFIX;
	private boolean allowAnonymous = DEFAULT_ALLOW_ANONYMOUS;
	private boolean requiresApproval = DEFAULT_REQUIRES_APPROVAL;
	private List<String> moderatorEmails = DEFAULT_MODERATOR_EMAILS;
	private List<AnonymousCommentField> requiredForAnonymous = DEFAULT_REQUIRED_FOR_ANONYMOUS;
	private Map<String, ProjectConfiguration> projectConfig = new HashMap<>();
	private boolean cacheTemplates = DEFAULT_CACHE_TEMPLATES;
	private String nodeConfigField = DEFAULT_NODE_CONFIG_FIELD;

	private MailConfig mailServerConfig = DEFAULT_MAIL_CONFIG;
	private String mailTemplateFolder = DEFAULT_MAIL_TEMPLATE_FOLDER;
	private String mailSender = DEFAULT_MAIL_SENDER;
	private Map<MailService.Template, String> mailSubjects = DEFAULT_MAIL_SUBJECTS;
	private LinkType mailResolveLinks = DEFAULT_MAIL_RESOLVE_LINKS;

	public Configuration() {
	}

	public String getCommentRoot() {
		return commentRoot;
	}

	public Configuration setCommentRoot(String commentRoot) {
		this.commentRoot = commentRoot;
		return this;
	}

	public String getSchemaPrefix() {
		return schemaPrefix;
	}

	public Configuration setSchemaPrefix(String schemaPrefix) {
		this.schemaPrefix = schemaPrefix;
		return this;
	}

	public List<String> getProjectNames() {
		return projectNames;
	}

	public Configuration setProjectNames(List<String> projectNames) {
		this.projectNames = projectNames;
		return this;
	}

	public boolean isRequiresApproval() {
		return requiresApproval;
	}

	public boolean isAllowAnonymous() {
		return allowAnonymous;
	}

	public Configuration setAllowAnonymous(boolean allowAnonymous) {
		this.allowAnonymous = allowAnonymous;
		return this;
	}

	public List<String> getModeratorEmails() {
		return moderatorEmails;
	}

	public Configuration setModeratorEmails(List<String> moderatorEmails) {
		this.moderatorEmails = moderatorEmails;
		return this;
	}

	public List<AnonymousCommentField> getRequiredForAnonymous() {
		return requiredForAnonymous;
	}

	public Configuration setRequiredForAnonymous(List<AnonymousCommentField> requiredForAnonymous) {
		this.requiredForAnonymous = requiredForAnonymous;
		return this;
	}

	public Map<String, ProjectConfiguration> getProjectConfig() {
		return projectConfig;
	}

	public Configuration setProjectConfig(Map<String, ProjectConfiguration> projectConfig) {
		this.projectConfig = projectConfig;
		return this;
	}

	public Configuration setRequiresApproval(boolean requiresApproval) {
		this.requiresApproval = requiresApproval;
		return this;
	}

	public boolean isCacheTemplates() {
		return cacheTemplates;
	}

	public Configuration setCacheTemplates(boolean cacheTemplates) {
		this.cacheTemplates = cacheTemplates;
		return this;
	}

	public MailConfig getMailServerConfig() {
		return mailServerConfig;
	}

	public Configuration setMailServerConfig(MailConfig mailServerConfig) {
		this.mailServerConfig = mailServerConfig;
		return this;
	}

	public String getMailTemplateFolder() {
		return mailTemplateFolder;
	}

	public Configuration setMailTemplateFolder(String mailTemplateFolder) {
		this.mailTemplateFolder = mailTemplateFolder;
		return this;
	}

	public String getMailSender() {
		return mailSender;
	}

	public Configuration setMailSender(String mailSender) {
		this.mailSender = mailSender;
		return this;
	}

	public Map<MailService.Template, String> getMailSubjects() {
		return mailSubjects;
	}

	public Configuration setMailSubjects(Map<MailService.Template, String> mailSubjects) {
		this.mailSubjects = mailSubjects;
		return this;
	}

	public String getNodeConfigField() {
		return nodeConfigField;
	}

	public Configuration setNodeConfigField(String nodeConfigField) {
		this.nodeConfigField = nodeConfigField;
		return this;
	}

	public LinkType getMailResolveLinks() {
		return mailResolveLinks;
	}

	public Configuration setMailResolveLinks(LinkType mailResolveLinks) {
		this.mailResolveLinks = mailResolveLinks;
		return this;
	}

	public String getLicenseKey() {
		return licenseKey;
	}

	public void setLicenseKey(String licenseKey) {
		this.licenseKey = licenseKey;
	}
}
