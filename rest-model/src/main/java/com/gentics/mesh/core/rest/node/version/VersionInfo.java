package com.gentics.mesh.core.rest.node.version;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import com.gentics.mesh.annotation.Setter;
import com.gentics.mesh.core.rest.user.UserReference;

/**
 * Response POJO which is used in {@link NodeVersionsResponse} to return information about a single version.
 */
public class VersionInfo {

	@JsonProperty(required = true)
	@JsonPropertyDescription("User reference of the creator of the element.")
	private UserReference creator;

	@JsonProperty(required = true)
	@JsonPropertyDescription("ISO8601 formatted created date string.")
	private String created;

	@JsonProperty(required = true)
	@JsonPropertyDescription("Version of the content.")
	private String version;

	@JsonProperty(required = true)
	@JsonPropertyDescription("Is the content a draft version?")
	private Boolean draft;

	@JsonProperty(required = true)
	@JsonPropertyDescription("Is the content published version?")
	private Boolean published;

	@JsonProperty(required = true)
	@JsonPropertyDescription("Is the version used as a root version in another branch?")
	private Boolean branchRoot;

	public VersionInfo() {
	}

	public String getCreated() {
		return created;
	}

	@Setter
	public VersionInfo setCreated(String created) {
		this.created = created;
		return this;
	}

	public UserReference getCreator() {
		return creator;
	}

	@Setter
	public VersionInfo setCreator(UserReference creator) {
		this.creator = creator;
		return this;
	}

	public String getVersion() {
		return version;
	}

	@Setter
	public VersionInfo setVersion(String version) {
		this.version = version;
		return this;
	}

	public Boolean getDraft() {
		return draft;
	}

	@Setter
	public VersionInfo setDraft(Boolean draft) {
		this.draft = draft;
		return this;
	}

	public Boolean getPublished() {
		return published;
	}

	@Setter
	public VersionInfo setPublished(Boolean published) {
		this.published = published;
		return this;
	}

	@Setter
	public VersionInfo setBranchRoot(Boolean branchRoot) {
		this.branchRoot = branchRoot;
		return this;
	}

	public Boolean getBranchRoot() {
		return branchRoot;
	}

}