package com.gentics.mesh.core.rest.release;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import com.gentics.mesh.core.rest.common.RestModel;

public class ReleaseCreateRequest implements RestModel {

	@JsonProperty(required = true)
	@JsonPropertyDescription("Name of the release")
	private String name;

	@JsonProperty(required = false)
	@JsonPropertyDescription("The hostname of the release which will be used to generate links across multiple projects.")
	private String hostname;

	@JsonProperty(required = false)
	@JsonPropertyDescription("SSL flag of the release which will be used to generate links across multiple projects.")
	private Boolean ssl;

	public ReleaseCreateRequest() {
	}

	/**
	 * Return the release name.
	 * 
	 * @return
	 */
	public String getName() {
		return name;
	}

	/**
	 * Set the release name.
	 * 
	 * @param name
	 * @return Fluent API
	 */
	public ReleaseCreateRequest setName(String name) {
		this.name = name;
		return this;
	}

	/**
	 * Return the configured hostname of release.
	 * 
	 * @return
	 */
	public String getHostname() {
		return hostname;
	}

	/**
	 * Set the hostname of the release.
	 * 
	 * @param hostname
	 * @return Fluent API
	 */
	public ReleaseCreateRequest setHostname(String hostname) {
		this.hostname = hostname;
		return this;
	}

	/**
	 * Return the ssl flag of the release.
	 * @return
	 */
	public Boolean getSsl() {
		return ssl;
	}

	/**
	 * Set the ssl flag of the release.
	 * 
	 * @param ssl
	 * @return Fluent API
	 */
	public ReleaseCreateRequest setSsl(Boolean ssl) {
		this.ssl = ssl;
		return this;
	}
}
