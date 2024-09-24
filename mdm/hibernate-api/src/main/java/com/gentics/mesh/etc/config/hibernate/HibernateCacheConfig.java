package com.gentics.mesh.etc.config.hibernate;

import org.apache.commons.lang3.StringUtils;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import com.gentics.mesh.annotation.Getter;
import com.gentics.mesh.annotation.Setter;
import com.gentics.mesh.etc.config.CacheConfig;
import com.gentics.mesh.etc.config.ConfigUtils;
import com.gentics.mesh.etc.config.MeshOptions;
import com.gentics.mesh.etc.config.env.EnvironmentVariable;

/**
 * Cache configuration for Mesh Hibernate
 */
public class HibernateCacheConfig extends CacheConfig {

	public static final String MESH_FIELD_CONTAINER_CACHE_SIZE = "MESH_FIELD_CONTAINER_CACHE_SIZE";

	private static final String DEFAULT_FIELD_CONTAINER_CACHE_SIZE = "50_000";

	public static final String MESH_LIST_FIELD_CACHE_SIZE = "MESH_LIST_FIELD_CACHE_SIZE";

	private static final String DEFAULT_MESH_LIST_FIELD_CACHE_SIZE = "50_000";

	/**
	 * Check whether a given configuration value matches any of the patterns for cache size
	 * @param config configuration value
	 * @return true if the value matches one of the patterns
	 */
	private static boolean matchesCacheSizeValue(String config) {
		if (StringUtils.isBlank(config)) {
			return true;
		}
		config = config.replace("_", "");
		return ConfigUtils.QUOTA_PATTERN_PERCENTAGE.matcher(config).matches()
				|| ConfigUtils.QUOTA_PATTERN_SIZE.matcher(config).matches()
				|| ConfigUtils.QUOTA_PATTERN_NUMBER.matcher(config).matches();
	}

	@JsonProperty(required = false)
	@JsonPropertyDescription("Set the maximum size of field container cache. A value of null will disable the cache. "
			+ "A value ended with K/M/G will mean an absolute value in [kil|mega|giga]bytes correspondingly. "
			+ "A value ended with % will mean a percent of maximum memory Mesh is allowed to use. "
			+ "Default: " + DEFAULT_FIELD_CONTAINER_CACHE_SIZE)
	@EnvironmentVariable(name = MESH_FIELD_CONTAINER_CACHE_SIZE, description = "Override the field container cache size.")
	private String fieldContainerCacheSize = DEFAULT_FIELD_CONTAINER_CACHE_SIZE;

	@JsonProperty(required = false)
	@JsonPropertyDescription("Set the maximum size of the list field cache. A value of null will disable the cache. "
			+ "A value ended with K/M/G will mean an absolute value in [kil|mega|giga]bytes correspondingly. "
			+ "A value ended with % will mean a percent of maximum memory Mesh is allowed to use. "
			+ "Default: " + DEFAULT_MESH_LIST_FIELD_CACHE_SIZE)
	@EnvironmentVariable(name = MESH_LIST_FIELD_CACHE_SIZE, description = "Override the  list field cache size.")
	private String listFieldCacheSize = DEFAULT_MESH_LIST_FIELD_CACHE_SIZE;

	public HibernateCacheConfig() {
	}

	@Getter
	public String getFieldContainerCacheSize() {
		return fieldContainerCacheSize;
	}

	@Setter
	public HibernateCacheConfig setFieldContainerCacheSize(String fieldContainerCacheSize) {
		this.fieldContainerCacheSize = fieldContainerCacheSize;
		if (StringUtils.isNotBlank(this.fieldContainerCacheSize)) {
			this.fieldContainerCacheSize = this.fieldContainerCacheSize.replace("_", "");
		}
		return this;
	}

	@Getter
	public String getListFieldCacheSize() {
		return listFieldCacheSize;
	}

	@Setter
	public HibernateCacheConfig setListFieldCacheSize(String listFieldCacheSize) {
		this.listFieldCacheSize = listFieldCacheSize;
		if (StringUtils.isNotBlank(this.listFieldCacheSize)) {
			this.listFieldCacheSize = this.listFieldCacheSize.replace("_", "");
		}
		return this;
	}

	@Override
	public void validate(MeshOptions options) {
		super.validate(options);
		if (StringUtils.isNotBlank(fieldContainerCacheSize)) {
			if (!matchesCacheSizeValue(fieldContainerCacheSize)) {
				throw new IllegalArgumentException("The fieldContainerCacheSize was set to " + fieldContainerCacheSize
						+ ", but is expected to either be a percentage (e.g. 10%) or an absolute memory size (like 10M) or an absolute number (like 10_000).");
			}
		}
		if (StringUtils.isNotBlank(listFieldCacheSize)) {
			if (!matchesCacheSizeValue(listFieldCacheSize)) {
				throw new IllegalArgumentException("The listFieldCacheSize was set to " + listFieldCacheSize
						+ ", but is expected to either be a percentage (e.g. 10%) or an absolute memory size (like 10M) or an absolute number (like 10_000).");
			}
		}
	}
}
