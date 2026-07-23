package com.gentics.mesh.core.rest.user;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import com.gentics.mesh.core.rest.common.RestModel;

/**
 * Request to create an API Token
 */
public class UserAPITokenCreateRequest implements RestModel {
	@JsonProperty(required = true)
	@JsonPropertyDescription("Name of the API Token.")
	private String name;

	@JsonProperty(required = false)
	@JsonPropertyDescription("ISO8601 formatted expire date string.")
	private String expires;

	/**
	 * Create an empty instance
	 */
	public UserAPITokenCreateRequest() {
	}

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
	public UserAPITokenCreateRequest setName(String name) {
		this.name = name;
		return this;
	}

	/**
	 * Get the ISO8601 formatted expire date
	 * @return expired date or null
	 */
	public String getExpires() {
		return expires;
	}

	/**
	 * Set the ISO8601 formatted expire date
	 * @param expires date or null
	 * @return fluent API
	 */
	public UserAPITokenCreateRequest setExpires(String expires) {
		this.expires = expires;
		return this;
	}
}
