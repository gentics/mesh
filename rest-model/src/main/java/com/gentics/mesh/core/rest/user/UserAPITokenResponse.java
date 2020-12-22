package com.gentics.mesh.core.rest.user;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import com.gentics.mesh.core.rest.common.RestModel;

/**
 * POJO for a REST API token response.
 */
public class UserAPITokenResponse implements RestModel {

	@JsonProperty(required = true)
	@JsonPropertyDescription("Issued client API token.")
	private String token;

	@JsonProperty(required = true)
	@JsonPropertyDescription("Date of the last time the API token was issued.")
	private String previousIssueDate;

	public UserAPITokenResponse() {
	}

	/**
	 * Return the API token.
	 * 
	 * @return
	 */
	public String getToken() {
		return token;
	}

	/**
	 * Set the token for the response.
	 * 
	 * @param token
	 * @return Fluent API
	 */
	public UserAPITokenResponse setToken(String token) {
		this.token = token;
		return this;
	}

	/**
	 * Returns the date the API token was last issued.
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

	public UserAPITokenResponse setPreviousIssueDate(String previousIssueDate) {
		this.previousIssueDate = previousIssueDate;
		return this;
	}

}
