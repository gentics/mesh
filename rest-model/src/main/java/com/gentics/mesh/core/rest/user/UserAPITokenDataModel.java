package com.gentics.mesh.core.rest.user;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import com.gentics.mesh.core.rest.common.AbstractResponse;

/**
 * REST Model for API Tokens
 */
public class UserAPITokenDataModel extends AbstractResponse {
	@JsonProperty(required = true)
	@JsonPropertyDescription("Name of the Token.")
	private String name;

	@JsonProperty(required = true)
	@JsonPropertyDescription("ISO8601 formatted issued date string.")
	private String issued;

	@JsonProperty(required = false)
	@JsonPropertyDescription("ISO8601 formatted last used date string.")
	private String lastUsed;

	@JsonProperty(required = false)
	@JsonPropertyDescription("ISO8601 formatted expire date string.")
	private String expires;

	@JsonProperty(required = true)
	@JsonPropertyDescription("True when the token is valid (not expired), false if not.")
	private boolean valid;

	/**
	 * Return the name
	 * @return name
	 */
	public String getName() {
		return name;
	}

	/**
	 * Set the name
	 * @param name name
	 * @return fluent API
	 */
	public UserAPITokenDataModel setName(String name) {
		this.name = name;
		return this;
	}

	/**
	 * Get the ISO8601 formatted issued date
	 * @return date
	 */
	public String getIssued() {
		return issued;
	}

	/**
	 * Set the ISO8601 formatted issued date
	 * @param issued date
	 * @return fluent API
	 */
	public UserAPITokenDataModel setIssued(String issued) {
		this.issued = issued;
		return this;
	}

	/**
	 * Get the ISO8601 formatted last used date or null
	 * @return date
	 */
	public String getLastUsed() {
		return lastUsed;
	}

	/**
	 * Set the ISO8601 formatted last sued date
	 * @param lastUsed date
	 * @return fluent API
	 */
	public UserAPITokenDataModel setLastUsed(String lastUsed) {
		this.lastUsed = lastUsed;
		return this;
	}

	/**
	 * Get the ISO8601 formatted expire date or null
	 * @return date
	 */
	public String getExpires() {
		return expires;
	}

	/**
	 * Set the ISO8601 formatted expire date
	 * @param expires date
	 * @return fluent API
	 */
	public UserAPITokenDataModel setExpires(String expires) {
		this.expires = expires;
		return this;
	}

	/**
	 * Get whether the token is valid (not expired)
	 * @return flag
	 */
	public boolean isValid() {
		return valid;
	}

	/**
	 * Set the valid flag
	 * @param valid flat
	 * @return fluent API
	 */
	public UserAPITokenDataModel setValid(boolean valid) {
		this.valid = valid;
		return this;
	}
}
