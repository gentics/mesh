package com.gentics.mesh.core.rest.branch;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import com.gentics.mesh.core.rest.common.AbstractGenericRestResponse;

/**
 * POJO for a branch response.
 */
public class BranchResponse extends AbstractGenericRestResponse {

	@JsonProperty(required = true)
	@JsonPropertyDescription("Name of the branch.")
	private String name;

	@JsonProperty(required = false)
	@JsonPropertyDescription("The hostname of the branch which will be used to generate links across multiple projects.")
	private String hostname;

	@JsonProperty(required = false)
	@JsonPropertyDescription("SSL flag of the branch which will be used to generate links across multiple projects.")
	private Boolean ssl;

	// private boolean active;

	@JsonProperty(required = true)
	@JsonPropertyDescription("Flag which indicates whether any active node migration for this branch is still running or whether all nodes have been migrated to this branch.")
	private boolean migrated;

	@JsonProperty(required = true)
	@JsonPropertyDescription("Flag which indicates whether this is the latest branch. Requests that do not specify a specific branch will be performed in the scope of the latest branch.")
	private boolean latest;

	public BranchResponse() {
	}

	/**
	 * Set the branch name.
	 * 
	 * @param name
	 * @return Fluent API
	 */
	public BranchResponse setName(String name) {
		this.name = name;
		return this;
	}

	/**
	 * Return the branch name.
	 * 
	 * @return
	 */
	public String getName() {
		return name;
	}

	// Active flag is currently not supported
	// @JsonIgnore
	// public boolean isActive() {
	// return active;
	// }
	//
	// public void setActive(boolean active) {
	// this.active = active;
	// }

	public boolean isMigrated() {
		return migrated;
	}

	public void setMigrated(boolean migrated) {
		this.migrated = migrated;
	}

	public String getHostname() {
		return hostname;
	}

	public void setHostname(String hostname) {
		this.hostname = hostname;
	}

	public Boolean getSsl() {
		return ssl;
	}

	public void setSsl(Boolean ssl) {
		this.ssl = ssl;
	}

	public boolean isLatest() {
		return latest;
	}

	public void setLatest(boolean latest) {
		this.latest = latest;
	}
}
