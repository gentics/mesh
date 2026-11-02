package com.gentics.mesh.etc.config;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.gentics.mesh.annotation.Getter;
import com.gentics.mesh.annotation.Setter;
import com.gentics.mesh.doc.GenerateDocumentation;
import com.gentics.mesh.etc.config.hibernate.HibernateCacheConfig;
import com.gentics.mesh.etc.config.hibernate.HibernateStorageOptions;

 /**
  * Top level Mesh Hibernate config POJO.
  * 
  * @author plyhun
  *
  */
@GenerateDocumentation
public class HibernateMeshOptions extends MeshOptions {

	@JsonProperty(required = true)
	@JsonPropertyDescription("Hibernate database options.")
	private HibernateStorageOptions storageOptions = new HibernateStorageOptions();

	@JsonProperty(required = true)
	@JsonPropertyDescription("Cache options.")
	private HibernateCacheConfig cacheConfig = new HibernateCacheConfig();

	@JsonProperty(required = true)
	@JsonPropertyDescription("OpenAPI options.")
	private OpenAPIOptions openApiOptions = new OpenAPIOptions();

	/**
	 * Validate this and the nested options.
	 */
	public void validate() {
		super.validate();
		if (getStorageOptions() != null) {
			getStorageOptions().validate(this);
		}
		if (getCacheConfig() != null) {
			getCacheConfig().validate(this);
		}
	}

	/**
	 * Get the storage configuration element.
	 * 
	 * @return
	 */
	public HibernateStorageOptions getStorageOptions() {
		return storageOptions;
	}

	public HibernateMeshOptions setStorageOptions(HibernateStorageOptions storageOptions) {
		this.storageOptions = storageOptions;
		return this;
	}

	@Override
	@Getter
	@JsonProperty("cache")
	@JsonDeserialize(as = HibernateCacheConfig.class)
	public HibernateCacheConfig getCacheConfig() {
		return cacheConfig;
	}

	@Override
	@Setter
	public MeshOptions setCacheConfig(CacheConfig cacheConfig) {
		this.cacheConfig = (HibernateCacheConfig) cacheConfig;
		return this;
	}

	@Override
	public void validate(MeshOptions options) {
		validate();
	}

	@Override
	@JsonIgnore
	public NativeQueryFiltering getNativeQueryFiltering() {
		return storageOptions.getNativeQueryFiltering();
	}

	@Override
	@JsonIgnore
	public MeshOptions setNativeQueryFiltering(NativeQueryFiltering nativeQueryFiltering) {
		storageOptions.setNativeQueryFiltering(nativeQueryFiltering);
		return this;
	}

	@Override
	@JsonIgnore
	public boolean hasDatabaseLevelCache() {
		return true;
	}

	/**
	 * Get the OpenAPI options
	 * @return options
	 */
	@JsonProperty("openApi")
	public OpenAPIOptions getOpenAPIOptions() {
		return openApiOptions;
	}

	/**
	 * Set the OpenAPI options
	 * @param openApiOptions options
	 * @return fluent API
	 */
	public MeshOptions setOpenAPIOptions(OpenAPIOptions openApiOptions) {
		this.openApiOptions = openApiOptions;
		return this;
	}

	@Override
	@JsonIgnore
	public Version getDefaultOpenAPIVersion() {
		return openApiOptions.getDefaultVersion();
	}

	@Override
	@JsonIgnore
	public Format getDefaultOpenAPIFormat() {
		return openApiOptions.getDefaultFormat();
	}

	@Override
	@JsonIgnore
	public String getNoApiInfoPlugins() {
		return openApiOptions.getNoApiInfoPlugins();
	}
}
