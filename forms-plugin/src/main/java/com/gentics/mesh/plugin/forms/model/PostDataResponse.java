package com.gentics.mesh.plugin.forms.model;

import java.util.Map;

public class PostDataResponse {
	private String dataUuid;

	private String created;

	private String redirectUrl;

	private boolean finished;

	Map<String, ValidationError> errors;

	public String getDataUuid() {
		return dataUuid;
	}

	public void setDataUuid(String dataUuid) {
		this.dataUuid = dataUuid;
	}

	public String getCreated() {
		return created;
	}

	public void setCreated(String created) {
		this.created = created;
	}

	public String getRedirectUrl() {
		return redirectUrl;
	}

	public void setRedirectUrl(String redirectUrl) {
		this.redirectUrl = redirectUrl;
	}

	public boolean isFinished() {
		return finished;
	}

	public void setFinished(boolean finished) {
		this.finished = finished;
	}

	public Map<String, ValidationError> getErrors() {
		return errors;
	}

	public void setErrors(Map<String, ValidationError> errors) {
		this.errors = errors;
	}
}
