package com.gentics.mesh.core.rest.release;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import com.gentics.mesh.core.rest.common.AbstractGenericRestResponse;

/**
 * POJO for a release response.
 */
public class ReleaseResponse extends AbstractGenericRestResponse {

	@JsonProperty(required = true)
	@JsonPropertyDescription("Name of the release")
	private String name;

	@JsonProperty(required = false)
	@JsonPropertyDescription("The hostname of the release which will be used to generate links across multiple projects.")
	private String hostname;

	@JsonProperty(required = false)
	@JsonPropertyDescription("SSL flag of the release which will be used to generate links across multiple projects.")
	private Boolean ssl;

	// private boolean active;

	@JsonProperty(required = true)
	@JsonPropertyDescription("Flag which indicates whether any active node migration for this release is still running or whether all nodes have been migrated to this release.")
	private boolean migrated;

	public ReleaseResponse() {
	}

	/**
	 * Set the release name.
	 * 
	 * @param name
	 * @return Fluent API
	 */
	public ReleaseResponse setName(String name) {
		this.name = name;
		return this;
	}

	/**
	 * Return the release name.
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
}
