package com.gentics.mesh.core.rest.branch;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import com.gentics.mesh.core.rest.common.RestModel;

public class BranchCreateRequest implements RestModel {

	@JsonProperty(required = true)
	@JsonPropertyDescription("Name of the branch.")
	private String name;

	@JsonProperty(required = false)
	@JsonPropertyDescription("The hostname of the branch which will be used to generate links across multiple projects.")
	private String hostname;

	@JsonProperty(required = false)
	@JsonPropertyDescription("SSL flag of the branch which will be used to generate links across multiple projects.")
	private Boolean ssl;

	public BranchCreateRequest() {
	}

	/**
	 * Return the branch name.
	 * 
	 * @return
	 */
	public String getName() {
		return name;
	}

	/**
	 * Set the branch name.
	 * 
	 * @param name
	 * @return Fluent API
	 */
	public BranchCreateRequest setName(String name) {
		this.name = name;
		return this;
	}

	/**
	 * Return the configured hostname of branch.
	 * 
	 * @return
	 */
	public String getHostname() {
		return hostname;
	}

	/**
	 * Set the hostname of the branch.
	 * 
	 * @param hostname
	 * @return Fluent API
	 */
	public BranchCreateRequest setHostname(String hostname) {
		this.hostname = hostname;
		return this;
	}

	/**
	 * Return the ssl flag of the branch.
	 * @return
	 */
	public Boolean getSsl() {
		return ssl;
	}

	/**
	 * Set the ssl flag of the branch.
	 * 
	 * @param ssl
	 * @return Fluent API
	 */
	public BranchCreateRequest setSsl(Boolean ssl) {
		this.ssl = ssl;
		return this;
	}
}
