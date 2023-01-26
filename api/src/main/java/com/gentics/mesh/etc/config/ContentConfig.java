package com.gentics.mesh.etc.config;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import com.gentics.mesh.doc.GenerateDocumentation;
import com.gentics.mesh.etc.config.env.EnvironmentVariable;
import com.gentics.mesh.etc.config.env.Option;

/**
 * Configuration options which are related to stored content.
 */
@GenerateDocumentation
public class ContentConfig implements Option {

	public static final String MESH_CONTENT_AUTO_PURGE_ENV = "MESH_CONTENT_AUTO_PURGE";
	public static final String MESH_CONTENT_BATCH_SIZE_ENV = "MESH_CONTENT_BATCH_SIZE";

	public static final boolean DEFAULT_AUTO_PURGE = true;
	public static final int DEFAULT_BATCH_SIZE = 5000;

	@JsonProperty(required = false)
	@JsonPropertyDescription("Flag which controls the global setting for the auto purge mechanism. The setting can be overriden by the schema 'autoPurge' flag. Default: "
		+ DEFAULT_AUTO_PURGE)
	@EnvironmentVariable(name = MESH_CONTENT_AUTO_PURGE_ENV, description = "Override the content versioning flag")
	private boolean autoPurge = DEFAULT_AUTO_PURGE;

	@JsonProperty(defaultValue = DEFAULT_BATCH_SIZE + " items")
	@JsonPropertyDescription("The size of a batch for operations over large amount of entities. Setting this to any number lower than 1 will disable batch chunking")
	@EnvironmentVariable(name = MESH_CONTENT_BATCH_SIZE_ENV, description = "Override the batch page size. Default: "
		+ DEFAULT_BATCH_SIZE)
	private int batchSize = DEFAULT_BATCH_SIZE;

	public ContentConfig() {

	}

	public int getBatchSize() {
		return batchSize;
	}

	public ContentConfig setBatchSize(int batchSize) {
		this.batchSize = batchSize;
		return this;
	}

	public boolean isAutoPurge() {
		return autoPurge;
	}

	public ContentConfig setAutoPurge(boolean autoPurge) {
		this.autoPurge = autoPurge;
		return this;
	}

	@Override
	public void validate(MeshOptions options) {
	}
}
