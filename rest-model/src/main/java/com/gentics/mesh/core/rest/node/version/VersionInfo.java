package com.gentics.mesh.core.rest.node.version;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import com.gentics.mesh.core.rest.user.UserReference;

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

	public VersionInfo() {
	}

	public String getCreated() {
		return created;
	}

	public void setCreated(String created) {
		this.created = created;
	}

	public UserReference getCreator() {
		return creator;
	}

	public void setCreator(UserReference creator) {
		this.creator = creator;
	}

	public String getVersion() {
		return version;
	}

	public void setVersion(String version) {
		this.version = version;
	}

}
