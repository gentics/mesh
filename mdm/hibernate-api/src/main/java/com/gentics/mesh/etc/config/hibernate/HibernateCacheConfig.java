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

	public static final String MESH_CLUSTERED_HIBERNATE_CACHE_HEAP_FREE = "MESH_CLUSTERED_HIBERNATE_CACHE_HEAP_FREE";

	private static final String DEFAULT_MESH_CLUSTERED_HIBERNATE_CACHE_HEAP_FREE = "10%";

	public static final String MESH_NON_CLUSTERED_HIBERNATE_CACHE_SIZE = "MESH_NON_CLUSTERED_HIBERNATE_CACHE_SIZE";

	private static final String DEFAULT_MESH_NON_CLUSTERED_HIBERNATE_CACHE_SIZE = "50_000";

	/**
	 * Check whether a given configuration value matches any of the patterns for cache size
	 * @param config configuration value
	 * @param allowCount true to allow counts, false only for memory based values
	 * @return true if the value matches one of the patterns
	 */
	private static boolean matchesCacheSizeValue(String config, boolean allowCount) {
		if (StringUtils.isBlank(config)) {
			return true;
		}
		config = config.replace("_", "");
		if (allowCount) {
			return ConfigUtils.QUOTA_PATTERN_PERCENTAGE.matcher(config).matches()
					|| ConfigUtils.QUOTA_PATTERN_SIZE.matcher(config).matches()
					|| ConfigUtils.QUOTA_PATTERN_NUMBER.matcher(config).matches();
		} else {
			return ConfigUtils.QUOTA_PATTERN_PERCENTAGE.matcher(config).matches()
					|| ConfigUtils.QUOTA_PATTERN_SIZE.matcher(config).matches();
		}
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

	@JsonProperty(required = false)
	@JsonPropertyDescription("Set the minimum required amount of free heap memory for the eviction policy of the hibernate cache when using clustering. "
			+ "A value ended with K/M/G will mean an absolute value in [kil|mega|giga]bytes correspondingly. "
			+ "A value ended with % will mean a percent of mininum free memory. "
			+ "Default: " + DEFAULT_MESH_CLUSTERED_HIBERNATE_CACHE_HEAP_FREE)
	@EnvironmentVariable(name = MESH_CLUSTERED_HIBERNATE_CACHE_HEAP_FREE, description = "Override the minimum required free heap memory for the eviction policy of the hibernate cache when using clustering.")
	private String clusteredHibernateCacheHeapFree = DEFAULT_MESH_CLUSTERED_HIBERNATE_CACHE_HEAP_FREE;

	@JsonProperty(required = false)
	@JsonPropertyDescription("Set the maximum size of the hibernate cache. A value of null will disable the cache. "
			+ "A value ended with K/M/G will mean an absolute value in [kil|mega|giga]bytes correspondingly. "
			+ "A value ended with % will mean a percent of maximum memory Mesh is allowed to use. "
			+ "Default: " + DEFAULT_MESH_NON_CLUSTERED_HIBERNATE_CACHE_SIZE)
	@EnvironmentVariable(name = MESH_NON_CLUSTERED_HIBERNATE_CACHE_SIZE, description = "Override the field container cache size.")
	private String nonClusteredHibernateCacheSize = DEFAULT_MESH_NON_CLUSTERED_HIBERNATE_CACHE_SIZE;

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

	@Getter
	public String getClusteredHibernateCacheHeapFree() {
		return clusteredHibernateCacheHeapFree;
	}

	@Setter
	public HibernateCacheConfig setClusteredHibernateCacheHeapFree(String clusteredHibernateCacheHeapFree) {
		this.clusteredHibernateCacheHeapFree = clusteredHibernateCacheHeapFree;
		return this;
	}

	@Getter
	public String getNonClusteredHibernateCacheSize() {
		return nonClusteredHibernateCacheSize;
	}

	@Setter
	public void setNonClusteredHibernateCacheSize(String nonClusteredHibernateCacheSize) {
		this.nonClusteredHibernateCacheSize = nonClusteredHibernateCacheSize;
		if (StringUtils.isNotBlank(this.fieldContainerCacheSize)) {
			this.nonClusteredHibernateCacheSize = this.nonClusteredHibernateCacheSize.replace("_", "");
		}
	}

	@Override
	public void validate(MeshOptions options) {
		super.validate(options);
		if (StringUtils.isNotBlank(fieldContainerCacheSize)) {
			if (!matchesCacheSizeValue(fieldContainerCacheSize, true)) {
				throw new IllegalArgumentException("The fieldContainerCacheSize was set to " + fieldContainerCacheSize
						+ ", but is expected to either be a percentage (e.g. 10%) or an absolute memory size (like 10M) or an absolute number (like 10_000).");
			}
		}
		if (StringUtils.isNotBlank(listFieldCacheSize)) {
			if (!matchesCacheSizeValue(listFieldCacheSize, true)) {
				throw new IllegalArgumentException("The listFieldCacheSize was set to " + listFieldCacheSize
						+ ", but is expected to either be a percentage (e.g. 10%) or an absolute memory size (like 10M) or an absolute number (like 10_000).");
			}
		}
		if (StringUtils.isNotBlank(clusteredHibernateCacheHeapFree)) {
			if (!matchesCacheSizeValue(clusteredHibernateCacheHeapFree, false)) {
				throw new IllegalArgumentException("The clusteredHibernateCacheHeapFree was set to " + clusteredHibernateCacheHeapFree
						+ ", but is expected to either be a percentage (e.g. 10%) or an absolute memory size (like 10M).");
			}
		}
		if (StringUtils.isNotBlank(nonClusteredHibernateCacheSize)) {
			if (!matchesCacheSizeValue(nonClusteredHibernateCacheSize, true)) {
				throw new IllegalArgumentException("The nonClusteredHibernateCacheSize was set to " + nonClusteredHibernateCacheSize
						+ ", but is expected to either be a percentage (e.g. 10%) or an absolute memory size (like 10M) or an absolute number (like 10_000).");
			}
		}
	}
}
