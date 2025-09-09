package com.gentics.mesh.plugin.forms.config;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.gentics.mesh.plugin.common.license.LicenseConfiguration;
import com.gentics.mesh.plugin.forms.export.ExportFormat;

import javax.annotation.Nonnull;
import java.util.HashMap;
import java.util.Map;

/**
 * Configuration POJO
 */
public class FormsConfig extends LicenseConfiguration {

	private String fromAddress;

	private String adminMailTemplate;

	private String adminMailSubject;

	private String userMailTemplate;

	private String userMailSubject;

	private int downloadPurgeInterval = 1;

	private ExportFormat defaultExportFormat = ExportFormat.CSV;

	private ReCaptchaOptions reCaptcha;

	private FriendlyCaptchaOptions friendlyCaptcha = new FriendlyCaptchaOptions();

	private EyepinOptions eyepin;

	private FileUploadOptions fileUpload;

	private ProxyOptions proxyOptions;

	private String templatesPath = "plugins/forms/templates";

	private Map<String, String> elements;

	private Map<String, Object> custom = new HashMap<>();

	@JsonProperty("mail")
	private MailOptions mailOptions = new MailOptions();

	private boolean validateElements = false;

	@JsonProperty("poll")
	private PollOptions pollOptions = new PollOptions();

	private MultipageRenderMode multipageRenderMode = MultipageRenderMode.SKIP_COMPLETED;

	@JsonProperty("export")
	private ExportOptions exportOptions = new ExportOptions();

	@JsonProperty("security")
	private SecurityOptions securityOptions = new SecurityOptions();

	private boolean testing;

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

	public int getDownloadPurgeInterval() {
		return downloadPurgeInterval;
	}

	public FormsConfig setDownloadPurgeInterval(int downloadPurgeInterval) {
		this.downloadPurgeInterval = downloadPurgeInterval;

		return this;
	}

	public void setDefaultExportFormat(ExportFormat defaultExportFormat) {
		this.defaultExportFormat = defaultExportFormat;
	}

	public ExportFormat getDefaultExportFormat() {
		return defaultExportFormat;
	}

	public ReCaptchaOptions getReCaptcha() {
		return reCaptcha;
	}

	public void setReCaptcha(ReCaptchaOptions reCaptcha) {
		this.reCaptcha = reCaptcha;
	}

	public FriendlyCaptchaOptions getFriendlyCaptcha() {
		return friendlyCaptcha;
	}

	public void setFriendlyCaptcha(FriendlyCaptchaOptions friendlyCaptcha) {
		this.friendlyCaptcha = friendlyCaptcha;
	}

	public EyepinOptions getEyepin() {
		return eyepin;
	}

	public void setEyepin(EyepinOptions eyepin) {
		this.eyepin = eyepin;
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

	public Map<String, Object> getCustom() {
		if (custom == null) {
			custom = new HashMap<>();
		}
		return custom;
	}

	public void setCustom(Map<String, Object> custom) {
		this.custom = custom;
	}

	@Nonnull
	public PollOptions getPollOptions() {
		if (pollOptions == null) {
			pollOptions = new PollOptions();
		}

		return pollOptions;
	}

	public FormsConfig setPollOptions(PollOptions pollOptions) {
		this.pollOptions = pollOptions;

		return this;
	}

	public MultipageRenderMode getMultipageRenderMode() {
		if (multipageRenderMode == null) {
			multipageRenderMode = MultipageRenderMode.SKIP_COMPLETED;
		}

		return multipageRenderMode;
	}

	public void setMultipageRenderMode(MultipageRenderMode multipageRenderMode) {
		this.multipageRenderMode = multipageRenderMode;
	}

	@Nonnull
	public ExportOptions getExportOptions() {
		if (exportOptions == null) {
			exportOptions = new ExportOptions();
		}

		return exportOptions;
	}

	public FormsConfig setExportOptions(ExportOptions exportOptions) {
		this.exportOptions = exportOptions;

		return this;
	}

	public boolean isTesting() {
		return testing;
	}

	public FormsConfig setTesting(boolean testing) {
		this.testing = testing;

		return this;
	}

	@Nonnull
	public SecurityOptions getSecurityOptions() {
		if (securityOptions == null) {
			securityOptions = new SecurityOptions();
		}

		return securityOptions;
	}

	public FormsConfig setSecurityOptions(SecurityOptions securityOptions) {
		this.securityOptions = securityOptions;

		return this;
	}
}
