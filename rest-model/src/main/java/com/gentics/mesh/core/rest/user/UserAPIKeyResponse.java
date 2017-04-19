package com.gentics.mesh.core.rest.user;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import com.gentics.mesh.core.rest.common.RestModel;

public class UserAPIKeyResponse implements RestModel {

	@JsonProperty(required = true)
	@JsonPropertyDescription("Issued client API key.")
	private String apiKey;

	@JsonProperty(required = true)
	@JsonPropertyDescription("Date of the last time the API key was issued.")
	private String previousIssueDate;

	public UserAPIKeyResponse() {
	}

	public String getApiKey() {
		return apiKey;
	}

	/**
	 * 
	 * @param apiKey
	 * @return Fluent API
	 */
	public UserAPIKeyResponse setApiKey(String apiKey) {
		this.apiKey = apiKey;
		return this;
	}

	/**
	 * Returns the date the api key was last issued.
	 * 
	 * @return
	 */
	public String getPreviousIssueDate() {
		return previousIssueDate;
	}

	/**
	 * Set the issue date.
	 * 
	 * @param previousIssueDate
	 * @return Fluent API
	 */
	
	public UserAPIKeyResponse setPreviousIssueDate(String previousIssueDate) {
		this.previousIssueDate = previousIssueDate;
		return this;
	}

}
