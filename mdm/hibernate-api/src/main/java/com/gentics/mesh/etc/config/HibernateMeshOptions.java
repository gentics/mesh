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
import com.gentics.mesh.etc.config.CacheConfig;
import com.gentics.mesh.etc.config.MeshOptions;
import com.gentics.mesh.etc.config.NativeQueryFiltering;
import com.gentics.mesh.etc.config.env.EnvironmentVariable;

 /**
  * Top level Mesh Hibernate config POJO.
  * 
  * @author plyhun
  *
  */
@GenerateDocumentation
public class HibernateMeshOptions extends MeshOptions {

	public static final String LICENSE_KEY_ENV = "LICENSEKEY";

	public static final String LICENSE_KEY_PATH_ENV = "LICENSEKEY_PATH";

	@JsonProperty(required = true)
	@JsonPropertyDescription("Hibernate database options.")
	private HibernateStorageOptions storageOptions = new HibernateStorageOptions();

	@JsonProperty(required = true)
	@JsonPropertyDescription("Cache options.")
	private HibernateCacheConfig cacheConfig = new HibernateCacheConfig();

	@JsonProperty(defaultValue = "license.key")
	@JsonPropertyDescription("Path to the license key file")
	@EnvironmentVariable(name = LICENSE_KEY_PATH_ENV, description = "Override the default licence key file path.")
	private String licenseKeyPath = "license.key";

	@JsonProperty
	@JsonPropertyDescription("The license key")
	@EnvironmentVariable(name = LICENSE_KEY_ENV, description = "The license key")
	private String licenseKey;

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

	public String getLicenseKey() {
		return licenseKey;
	}

	public void setLicenseKey(String licenseKey) {
		this.licenseKey = licenseKey;
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

	public String getLicenseKeyPath() {
		return licenseKeyPath;
	}

	public void setLicenseKeyPath(String licenseKeyPath) {
		this.licenseKeyPath = licenseKeyPath;
	}

	@Override
	@JsonIgnore
	public boolean hasDatabaseLevelCache() {
		return true;
	}
}
