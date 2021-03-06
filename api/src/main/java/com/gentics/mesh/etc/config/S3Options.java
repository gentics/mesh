package com.gentics.mesh.etc.config;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import com.gentics.mesh.annotation.Setter;
import com.gentics.mesh.doc.GenerateDocumentation;
import com.gentics.mesh.etc.config.env.EnvironmentVariable;
import com.gentics.mesh.etc.config.env.Option;

import java.util.Set;

/**
 * S3 configuration POJO.
 */
@GenerateDocumentation
public class S3Options implements Option {
	public static final boolean ENABLED = true;
	public static final boolean DISABLED = false;

	public static final boolean DEFAULT_S3_MODE = DISABLED;
	public static final int DEFAULT_EXPIRATION_TIME_UPLOAD = 60_000;
	public static final int DEFAULT_EXPIRATION_TIME_DOWNLOAD = 360_000;
	public static final int DEFAULT_PARSER_LIMIT = 40_000;

	public static final String MESH_S3_BINARY_ENABLED_KEY_ENV = "MESH_S3_BINARY_ENABLED_KEY";
	public static final String MESH_S3_BINARY_SECRET_ACCESS_KEY_ENV = "MESH_S3_BINARY_SECRET_ACCESS_KEY";
	public static final String MESH_S3_BINARY_ACCESS_KEY_ID_ENV = "MESH_S3_BINARY_ACCESS_KEY_ID";
	public static final String MESH_S3_BINARY_ENDPOINT_ENV = "MESH_S3_BINARY_ENDPOINT";
	public static final String MESH_S3_BINARY_BUCKET_ENV = "MESH_S3_BINARY_BUCKET";
	public static final String MESH_S3_BINARY_EXPIRATION_TIME_UPLOAD_ENV = "MESH_S3_BINARY_EXPIRATION_TIME_UPLOAD";
	public static final String MESH_S3_BINARY_EXPIRATION_TIME_DOWNLOAD_ENV = "MESH_S3_BINARY_EXPIRATION_TIME_DOWNLOAD";
	public static final String MESH_S3_BINARY_LINK_RESOLVER_ENV = "MESH_S3_BINARY_LINK_RESOLVER";
	public static final String MESH_S3_BINARY_METADATA_WHITELIST_ENV = "MESH_S3_BINARY_METADATA_WHITELIST";
	public static final String MESH_S3_BINARY_PARSER_LIMIT_ENV = "MESH_S3_BINARY_PARSER_LIMIT";
	public static final String MESH_S3_BINARY_REGION_ENV = "MESH_S3_BINARY_REGION";

	@JsonProperty(required = false)
	@JsonPropertyDescription("Flag to enable or disable the S3 engine.")
	@EnvironmentVariable(name = MESH_S3_BINARY_ENABLED_KEY_ENV, description = "Override cluster enabled flag.")
	private boolean enabled = DEFAULT_S3_MODE;

	@JsonProperty(required = false)
	@JsonPropertyDescription("AWS S3 bucket where binaries will be uploaded to and downloaded from")
	@EnvironmentVariable(name = MESH_S3_BINARY_BUCKET_ENV, description = "Override the configured AWS S3 bucket.")
	private String bucket;

	@JsonProperty(required = false)
	@JsonPropertyDescription("After this time in milliseconds the AWS S3 URL used for upload will expire.")
	@EnvironmentVariable(name = MESH_S3_BINARY_EXPIRATION_TIME_UPLOAD_ENV, description = "Override the configured AWS S3 upload time.")
	private int expirationTimeUpload = DEFAULT_EXPIRATION_TIME_UPLOAD;

	@JsonProperty(required = false)
	@JsonPropertyDescription("After this time in milliseconds the AWS S3 URL used for download will expire.")
	@EnvironmentVariable(name = MESH_S3_BINARY_EXPIRATION_TIME_DOWNLOAD_ENV, description = "Override the configured AWS S3 download time.")
	private int expirationTimeDownload = DEFAULT_EXPIRATION_TIME_DOWNLOAD;

	@JsonProperty(required = false)
	@JsonPropertyDescription("Style of linkResolving:'s3' (default) resolved Links will render a pre-signed URL directly to S3 OR 'mesh' resolved Links will render a mesh-url")
	@EnvironmentVariable(name = MESH_S3_BINARY_LINK_RESOLVER_ENV, description = "Override the configured AWS S3 link resolver.")
	private String linkResolver;

	@JsonProperty(required = false)
	@JsonPropertyDescription("If set, the parser will only extract metadata with the keys specified in the list.")
	@EnvironmentVariable(name = MESH_S3_BINARY_METADATA_WHITELIST_ENV, description = "Override the metadata whitelist")
	private Set<String> metadataWhitelist;

	@JsonProperty(required = false)
	@JsonPropertyDescription("The parser limit for uploaded documents (pdf, doc, docx). ")
	@EnvironmentVariable(name = MESH_S3_BINARY_PARSER_LIMIT_ENV, description = "Override the configured parser limit.")
	private int parserLimit = DEFAULT_PARSER_LIMIT;

	@JsonProperty(required = false)
	@JsonPropertyDescription("AWS S3 region")
	@EnvironmentVariable(name = MESH_S3_BINARY_REGION_ENV, description = "Override the configured AWS S3 region.")
	private String region;

	@JsonProperty(required = false)
	@JsonPropertyDescription("AWS S3 secret access key")
	@EnvironmentVariable(name = MESH_S3_BINARY_SECRET_ACCESS_KEY_ENV, description = "Override the configured AWS S3 secret access key.")
	private String secretAccessKey;

	@JsonProperty(required = false)
	@JsonPropertyDescription("AWS S3 access key id")
	@EnvironmentVariable(name = MESH_S3_BINARY_ACCESS_KEY_ID_ENV, description = "Override the configured AWS S3 access key id.")
	private String accessKeyId;

	@JsonProperty(required = false)
	@JsonPropertyDescription("AWS S3 custom endpoint")
	@EnvironmentVariable(name = MESH_S3_BINARY_ENDPOINT_ENV, description = "Override the configured AWS S3 custom endpoint.")
	private String endpoint;

	@JsonProperty(required = true)
	@JsonPropertyDescription("S3 Cache Bucket Options.")
	private S3CacheOptions s3cacheOptions = new S3CacheOptions();

	@JsonProperty("s3cacheOptions")
	public S3CacheOptions getS3CacheOptions() {
		if (s3cacheOptions == null) {
			s3cacheOptions = new S3CacheOptions();
		}
		return this.s3cacheOptions;
	}

	@Setter
	public S3Options setS3CacheOptions(S3CacheOptions s3cacheOptions) {
		this.s3cacheOptions = s3cacheOptions;
		return this;
	}

	public String getSecretAccessKey() {
		return secretAccessKey;
	}

	@Setter
	public S3Options setSecretAccessKey(String secretAccessKey) {
		this.secretAccessKey = secretAccessKey;
		return this;
	}

	public String getAccessKeyId() {
		return accessKeyId;
	}

	@Setter
	public S3Options setAccessKeyId(String accessKeyId) {
		this.accessKeyId = accessKeyId;
		return this;
	}

	public String getRegion() {
		return region;
	}

	@Setter
	public S3Options setRegion(String region) {
		this.region = region;
		return this;
	}

	public String getEndpoint() {
		return endpoint;
	}

	@Setter
	public S3Options setEndpoint(String endpoint) {
		this.endpoint = endpoint;
		return this;
	}

	public String getBucket() {
		return bucket;
	}

	@Setter
	public S3Options setBucket(String bucket) {
		this.bucket = bucket;
		return this;
	}

	public int getExpirationTimeUpload() {
		return expirationTimeUpload;
	}

	@Setter
	public S3Options setExpirationTimeUpload(int expirationTimeUpload) {
		this.expirationTimeUpload = expirationTimeUpload;
		return this;
	}

	public int getExpirationTimeDownload() {
		return expirationTimeDownload;
	}

	@Setter
	public void setExpirationTimeDownload(int expirationTimeDownload) {
		this.expirationTimeDownload = expirationTimeDownload;
	}

	public boolean isEnabled() {
		return enabled;
	}

	@Setter
	public S3Options setEnabled(boolean enabled) {
		this.enabled = enabled;
		return this;
	}

	@Setter
	public String getLinkResolver() {
		return linkResolver;
	}

	@Setter
	public void setLinkResolver(String linkResolver) {
		this.linkResolver = linkResolver;
	}

	public Set<String> getMetadataWhitelist() {
		return metadataWhitelist;
	}

	@Setter
	public void setMetadataWhitelist(Set<String> metadataWhitelist) {
		this.metadataWhitelist = metadataWhitelist;
	}

	public int getParserLimit() {
		return parserLimit;
	}

	@Setter
	public void setParserLimit(int parserLimit) {
		this.parserLimit = parserLimit;
	}

	/**
	 * Validate the settings.
	 */
	public void validate(MeshOptions meshOptions) {
		if (isEnabled()) {
			if (getAccessKeyId() == null || getSecretAccessKey() == null || getBucket() == null
					|| getRegion() == null) {
				throw new NullPointerException(
						"You have not specified the required S3 parameters: accessKeyId, secretAccessKey, bucket, region. Please either fill in the required parameters or disable S3 support.");
			}
		}
	}
}
