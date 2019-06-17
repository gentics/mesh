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

	@JsonProperty(required = false, defaultValue = "true")
	@JsonPropertyDescription("Whether the new branch will be set as 'latest' branch. Defaults to 'true'.")
	private boolean latest = true;

	@JsonProperty(required = false)
	@JsonPropertyDescription("Optional reference to the base branch. If not set, the new branch will be based on the current 'latest' branch.")
	private BranchReference baseBranch;

	@JsonProperty(required = false)
	@JsonPropertyDescription("Optional path prefix for webroot path and rendered links.")
	private String pathPrefix;

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
	 * 
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

	/**
	 * Return whether the branch shall be made the latest branch.
	 * 
	 * @return
	 */
	public boolean isLatest() {
		return latest;
	}

	/**
	 * Set whether to make the branch the latest branch
	 * 
	 * @param latest
	 * @return Fluent API
	 */
	public BranchCreateRequest setLatest(boolean latest) {
		this.latest = latest;
		return this;
	}

	/**
	 * Get the base branch
	 * 
	 * @return base branch
	 */
	public BranchReference getBaseBranch() {
		return baseBranch;
	}

	/**
	 * Set the base branch
	 * 
	 * @param baseBranch
	 *            base branch
	 * @return Fluent API
	 */
	public BranchCreateRequest setBaseBranch(BranchReference baseBranch) {
		this.baseBranch = baseBranch;
		return this;
	}

	/**
	 * Return the path prefix.
	 * 
	 * @return
	 */
	public String getPathPrefix() {
		return pathPrefix;
	}

	/**
	 * Set the path prefix.
	 * 
	 * @param pathPrefix
	 * @return Fluent API
	 */
	public BranchCreateRequest setPathPrefix(String pathPrefix) {
		this.pathPrefix = pathPrefix;
		return this;
	}
}
