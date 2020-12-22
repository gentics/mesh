package com.gentics.mesh.etc.config;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import com.gentics.mesh.annotation.Setter;
import com.gentics.mesh.doc.GenerateDocumentation;
import com.gentics.mesh.etc.config.env.EnvironmentVariable;
import com.gentics.mesh.etc.config.env.Option;

/**
 * Configuration options which are related to stored content.
 */
@GenerateDocumentation
public class ContentConfig implements Option {

	public static final String MESH_CONTENT_AUTO_PURGE_ENV = "MESH_CONTENT_AUTO_PURGE";

	private static final boolean DEFAULT_AUTO_PURGE = true;

	@JsonProperty(required = false)
	@JsonPropertyDescription("Flag which controls the global setting for the auto purge mechanism. The setting can be overriden by the schema 'autoPurge' flag. Default: "
		+ DEFAULT_AUTO_PURGE)
	@EnvironmentVariable(name = MESH_CONTENT_AUTO_PURGE_ENV, description = "Override the content versioning flag")
	private boolean autoPurge = DEFAULT_AUTO_PURGE;

	public ContentConfig() {

	}

	public boolean isAutoPurge() {
		return autoPurge;
	}

	@Setter
	public ContentConfig setAutoPurge(boolean autoPurge) {
		this.autoPurge = autoPurge;
		return this;
	}

	@Override
	public void validate(AbstractMeshOptions options) {
	}
}
