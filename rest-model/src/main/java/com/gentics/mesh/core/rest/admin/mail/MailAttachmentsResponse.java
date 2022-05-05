package com.gentics.mesh.core.rest.admin.mail;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;

public class MailAttachmentsResponse {

	@JsonProperty(required = true)
	@JsonPropertyDescription("The project where the attachment is")
	private String project;

	@JsonProperty(required = true)
	@JsonPropertyDescription("The uuid of the attachment")
	private String uuid;

	@JsonProperty(required = true)
	@JsonPropertyDescription("The language of the attachment")
	private String language;

	@JsonProperty(required = true)
	@JsonPropertyDescription("The field of the schema where the attachment is")
	private String field;

	@JsonProperty(required = false)
	@JsonPropertyDescription("The branch of the attachment. If not set the default will be taken")
	private String branch;

	public String getProject() {
		return project;
	}

	public void setProject(String project) {
		this.project = project;
	}

	public String getUuid() {
		return uuid;
	}

	public void setUuid(String uuid) {
		this.uuid = uuid;
	}

	public String getLanguage() {
		return language;
	}

	public void setLanguage(String language) {
		this.language = language;
	}

	public String getField() {
		return field;
	}

	public void setField(String field) {
		this.field = field;
	}

	public String getBranch() {
		return branch;
	}

	public void setBranch(String branch) {
		this.branch = branch;
	}

}
