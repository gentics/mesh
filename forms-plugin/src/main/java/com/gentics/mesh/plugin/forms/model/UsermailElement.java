package com.gentics.mesh.plugin.forms.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import com.gentics.mesh.core.rest.node.NodeResponse;
import io.vertx.core.MultiMap;
import io.vertx.ext.web.FileUpload;
import org.apache.commons.lang3.StringUtils;

import java.util.Collections;
import java.util.Set;

/**
 * Usermail element
 */
@Reportable
public class UsermailElement extends FormElement {
	@JsonProperty("mailsubject")
	private String subject;

	@JsonProperty("mailtemplate")
	@JsonPropertyDescription("Path to the e-mail template")
	private String template;

	@JsonProperty
	private String mailsource;

	public UsermailElement() {
	}

	protected UsermailElement(UsermailElement other, boolean deep) {
		super(other, deep);

		this.subject = other.subject;
		this.template = other.template;
		this.mailsource = other.mailsource;
	}

	@JsonProperty("mailsubject")
	public String getSubject() {
		return subject;
	}

	@JsonProperty("mailsubject")
	public void setSubject(String subject) {
		this.subject = subject;
	}

	/**
	 * Get the path to the user mail template.
	 * @return The path to the user mail template.
	 */
	@JsonProperty("mailtemplate")
	public String getTemplate() {
		return template;
	}

	/**
	 * Set the path to the user mail template.
	 */
	@JsonProperty("mailtemplate")
	public void setTemplate(String template) {
		this.template = template;
	}

	/**
	 * Get the user mail template source.
	 * @return The user mail template source.
	 */
	public String getMailsource() {
		return mailsource;
	}

	/**
	 * Set the user mail template source.
	 */
	public UsermailElement setMailsource(String mailsource) {
		this.mailsource = mailsource;
		return this;
	}

	@Override
	public ValidationError validate(MultiMap postedData, Set<FileUpload> fileUploads, NodeResponse formDataNode) {
		ValidationError error = super.validate(postedData, fileUploads, formDataNode);
		if (error == null) {
			String value = getSingleFormData(postedData);
			if (StringUtils.isNotBlank(value)) {
				error = Validator.email.validate(getName(), Collections.singletonList(value));
			}
		}
		return error;
	}

	@Override
	protected FormElement clone(boolean deep) {
		return new UsermailElement(this, deep);
	}
}
