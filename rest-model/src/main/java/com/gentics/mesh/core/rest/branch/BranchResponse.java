package com.gentics.mesh.core.rest.branch;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import com.gentics.mesh.core.rest.common.AbstractGenericRestResponse;
import com.gentics.mesh.core.rest.tag.TagReference;

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

	@JsonProperty(required = true)
	@JsonPropertyDescription("Optional path prefix for webroot path and rendered links.")
	private String pathPrefix;

	// private boolean active;

	@JsonProperty(required = true)
	@JsonPropertyDescription("Flag which indicates whether any active node migration for this branch is still running or whether all nodes have been migrated to this branch.")
	private Boolean migrated;

	@JsonProperty(required = true)
	@JsonPropertyDescription("Flag which indicates whether this is the latest branch. Requests that do not specify a specific branch will be performed in the scope of the latest branch.")
	private Boolean latest;

	@JsonProperty(required = true)
	@JsonPropertyDescription("List of tags that were used to tag the branch.")
	private List<TagReference> tags;

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

	/**
	 * Get the migration status for the branch.
	 * 
	 * @return
	 * @deprecated Use {@link #getMigrated()} instead.
	 */
	@JsonIgnore
	@Deprecated
	public boolean isMigrated() {
		return migrated != null ? migrated : false;
	}

	public Boolean getMigrated() {
		return migrated;
	}

	public void setMigrated(Boolean migrated) {
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

	public Boolean getLatest() {
		return latest;
	}

	public void setLatest(Boolean latest) {
		this.latest = latest;
	}

	/**
	 * Return the tags which were used to tag the branch. The tags are nested within their tag families.
	 * 
	 * @return
	 */
	public List<TagReference> getTags() {
		return tags;
	}

	public void setTags(List<TagReference> tags) {
		this.tags = tags;
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
	public BranchResponse setPathPrefix(String pathPrefix) {
		this.pathPrefix = pathPrefix;
		return this;
	}

	/**
	 * Create a new {@link BranchUpdateRequest} from the values of this response.
	 * 
	 * @return
	 */
	public BranchUpdateRequest toRequest() {
		return new BranchUpdateRequest()
			.setName(getName())
			.setPathPrefix(getPathPrefix())
			.setHostname(getHostname())
			.setSsl(getSsl());
	}
}
