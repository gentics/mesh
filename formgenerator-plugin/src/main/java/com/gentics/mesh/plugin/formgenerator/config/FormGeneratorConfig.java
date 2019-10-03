package com.gentics.mesh.plugin.formgenerator.config;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.gentics.mesh.plugin.formgenerator.export.ExportFormat;

import java.util.List;
import java.util.Map;

public class FormGeneratorConfig {

	private String apiKey;

	private String fromAddress;

	private String adminMailTemplate;

	private String adminMailSubject;

	private String userMailTemplate;

	private String userMailSubject;

	private ExportFormat defaultExportFormat = ExportFormat.CSV;

	private ReCaptchaOptions reCaptcha;

	private FileUploadOptions fileUpload;

	private ProxyOptions proxyOptions;

	private List<String> projects;

	private String templatesPath;

	private Map<String, String> elements;

	@JsonProperty("mail")
	private MailOptions mailOptions = new MailOptions();

	private boolean validateElements = false;

	public String getApiKey() {
		return apiKey;
	}

	public void setApiKey(String apiKey) {
		this.apiKey = apiKey;
	}

	public String getFromAddress() {
		return fromAddress;
	}

	public void setFromAddress(String fromAddress) {
		this.fromAddress = fromAddress;
	}

	public String getAdminMailTemplate() {
		return adminMailTemplate;
	}

	public void setAdminMailTemplate(String adminMailTemplate) {
		this.adminMailTemplate = adminMailTemplate;
	}

	public String getAdminMailSubject() {
		return adminMailSubject;
	}

	public void setAdminMailSubject(String adminMailSubject) {
		this.adminMailSubject = adminMailSubject;
	}

	public String getUserMailTemplate() {
		return userMailTemplate;
	}

	public void setUserMailTemplate(String userMailTemplate) {
		this.userMailTemplate = userMailTemplate;
	}

	public String getUserMailSubject() {
		return userMailSubject;
	}

	public void setUserMailSubject(String userMailSubject) {
		this.userMailSubject = userMailSubject;
	}

	public ExportFormat getDefaultExportFormat() {
		return defaultExportFormat;
	}

	public ReCaptchaOptions getReCaptcha() {
		return reCaptcha;
	}

	public void setDefaultExportFormat(ExportFormat defaultExportFormat) {
		this.defaultExportFormat = defaultExportFormat;
	}

	public void setReCaptcha(ReCaptchaOptions reCaptcha) {
		this.reCaptcha = reCaptcha;
	}

	public FileUploadOptions getFileUpload() {
		return fileUpload;
	}

	public void setFileUpload(FileUploadOptions fileUpload) {
		this.fileUpload = fileUpload;
	}

	public ProxyOptions getProxyOptions() {
		return proxyOptions;
	}

	public void setProxyOptions(ProxyOptions proxyOptions) {
		this.proxyOptions = proxyOptions;
	}

	public List<String> getProjects() {
		return projects;
	}

	public void setProjects(List<String> projects) {
		this.projects = projects;
	}

	public String getTemplatesPath() {
		return templatesPath;
	}

	public void setTemplatesPath(String templatesPath) {
		this.templatesPath = templatesPath;
	}

	@JsonProperty("mail")
	public MailOptions getMailOptions() {
		return mailOptions;
	}

	@JsonProperty("mail")
	public void setMailOptions(MailOptions mailOptions) {
		this.mailOptions = mailOptions;
	}

	public Map<String, String> getElements() {
		return elements;
	}

	public void setElements(Map<String, String> elements) {
		this.elements = elements;
	}

	public boolean isValidateElements() {
		return validateElements;
	}

	public void setValidateElements(boolean validateElements) {
		this.validateElements = validateElements;
	}
}
