package com.gentics.mesh.etc.config;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import com.gentics.mesh.doc.GenerateDocumentation;
import com.gentics.mesh.etc.config.env.EnvironmentVariable;
import com.gentics.mesh.etc.config.env.Option;

@GenerateDocumentation
public class ContentConfig implements Option {

	public static final String MESH_CONTENT_VERSIONING_ENV = "MESH_CONTENT_VERSIONING_ENABLED";

	private static final boolean DEFAULT_VERSIONING = false;

	@JsonProperty(required = false)
	@JsonPropertyDescription("Flag which controls the default value for the versioning system. The default value can be overriden by the schema 'versioned' flag. Default: "
		+ DEFAULT_VERSIONING)
	@EnvironmentVariable(name = MESH_CONTENT_VERSIONING_ENV, description = "Override the content versioning flag")
	private boolean versioning = DEFAULT_VERSIONING;

	public ContentConfig() {

	}

	public boolean isVersioning() {
		return versioning;
	}

	public void setVersioning(boolean versioning) {
		this.versioning = versioning;
	}

	@Override
	public void validate(MeshOptions options) {
	}
}
