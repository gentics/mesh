package com.gentics.mesh.core.rest.user;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import com.gentics.mesh.core.rest.common.RestModel;

public class UserAPIKeyResponse implements RestModel {

	@JsonProperty(required = true)
	@JsonPropertyDescription("Issued client API key.")
	private String apiKey;

	public UserAPIKeyResponse() {
	}

	public String getApiKey() {
		return apiKey;
	}

	public UserAPIKeyResponse setApiKey(String apiKey) {
		this.apiKey = apiKey;
		return this;
	}
}
