package com.gentics.mesh.storage.s3;

import java.util.Objects;

import com.gentics.mesh.annotation.Setter;

/**
 * S3 Storage options.
 */
public class S3StorageOptions {

	public static final String DEFAULT_BUCKET_NAME = "mesh";

	private String url;

	private String accessId;

	private String accessKey;

	private String bucketName = DEFAULT_BUCKET_NAME;

	private String region;

	public String getUrl() {
		return url;
	}

	@Setter
	public S3StorageOptions setUrl(String url) {
		this.url = url;
		return this;
	}

	public String getAccessId() {
		return accessId;
	}

	@Setter
	public S3StorageOptions setAccessId(String accessId) {
		this.accessId = accessId;
		return this;
	}

	public String getAccessKey() {
		return accessKey;
	}

	@Setter
	public S3StorageOptions setAccessKey(String accessKey) {
		this.accessKey = accessKey;
		return this;
	}

	public String getRegion() {
		return region;
	}

	@Setter
	public S3StorageOptions setRegion(String region) {
		this.region = region;
		return this;
	}

	public String getBucketName() {
		return bucketName;
	}

	@Setter
	public S3StorageOptions setBucketName(String bucketName) {
		this.bucketName = bucketName;
		return this;
	}

	/**
	 * Validate the settings.
	 */
	public void validate() {
		Objects.requireNonNull(url, "No S3 URL has been specified");
		Objects.requireNonNull(accessId, "No accessId has been specified");
		Objects.requireNonNull(accessKey, "No accessKey has been specified");
		Objects.requireNonNull(region, "No region has been specified");
	}

}
