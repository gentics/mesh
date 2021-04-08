package com.gentics.mesh.plugin.forms.model;

import java.util.Arrays;
import java.util.Set;

import org.apache.commons.lang3.StringUtils;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.gentics.mesh.core.rest.node.NodeResponse;

import io.vertx.core.MultiMap;
import io.vertx.ext.web.FileUpload;

/**
 * Usermail element
 */
@Reportable
public class UsermailElement extends FormElement {
	@JsonProperty("mailsubject")
	private String subject;

	@JsonProperty("mailtemplate")
	private String template;

	@JsonProperty("mailsubject")
	public String getSubject() {
		return subject;
	}

	@JsonProperty("mailsubject")
	public void setSubject(String subject) {
		this.subject = subject;
	}

	@JsonProperty("mailtemplate")
	public String getTemplate() {
		return template;
	}

	@JsonProperty("mailtemplate")
	public void setTemplate(String template) {
		this.template = template;
	}

	@Override
	public ValidationError validate(MultiMap postedData, Set<FileUpload> fileUploads, NodeResponse formDataNode) {
		ValidationError error = super.validate(postedData, fileUploads, formDataNode);
		if (error == null) {
			String value = getSingleFormData(postedData);
			if (StringUtils.isNotBlank(value)) {
				error = Validator.email.validate(getName(), Arrays.asList(value));
			}
		}
		return error;
	}
}
