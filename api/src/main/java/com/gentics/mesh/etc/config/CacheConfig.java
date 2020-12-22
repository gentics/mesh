package com.gentics.mesh.etc.config;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import com.gentics.mesh.annotation.Setter;
import com.gentics.mesh.doc.GenerateDocumentation;
import com.gentics.mesh.etc.config.env.EnvironmentVariable;
import com.gentics.mesh.etc.config.env.Option;

/**
 * Configuration POJO for caching options.
 */
@GenerateDocumentation
public class CacheConfig implements Option {

	public static final String MESH_CACHE_PATH_SIZE_ENV = "MESH_CACHE_PATH_SIZE";

	private static final long DEFAULT_PATH_CACHE_SIZE = 20_000;

	@JsonProperty(required = false)
	@JsonPropertyDescription("Set the maximum size of the path cache. A value of 0 will disable the cache. Default: "
		+ DEFAULT_PATH_CACHE_SIZE)
	@EnvironmentVariable(name = MESH_CACHE_PATH_SIZE_ENV, description = "Override the path cache size.")
	private long pathCacheSize = DEFAULT_PATH_CACHE_SIZE;

	public CacheConfig() {

	}

	public long getPathCacheSize() {
		return pathCacheSize;
	}

	@Setter
	public CacheConfig setPathCacheSize(long pathCacheSize) {
		this.pathCacheSize = pathCacheSize;
		return this;
	}

	@Override
	public void validate(MeshOptions options) {
	}

}
