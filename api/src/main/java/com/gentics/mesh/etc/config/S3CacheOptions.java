package com.gentics.mesh.etc.config;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import com.gentics.mesh.doc.GenerateDocumentation;
import com.gentics.mesh.etc.config.env.EnvironmentVariable;
import com.gentics.mesh.etc.config.env.Option;

/**
 * S3 Cache configuration POJO.
 */
@GenerateDocumentation
public class S3CacheOptions implements Option {

    public static final int DEFAULT_EXPIRATION_TIME_CACHE_DOWNLOAD = 360_000;
    public static final int DEFAULT_EXPIRATION_TIME_CACHE_UPLOAD = 60_000;

    public static final String MESH_S3_BINARY_CACHE_AWS_KEY_ENV = "MESH_S3_BINARY_CACHE_AWS_KEY";
    public static final String MESH_S3_BINARY_CACHE_BUCKET_ENV = "MESH_S3_BINARY_CACHE_BUCKET";
    public static final String MESH_S3_BINARY_CACHE_EXPIRATION_TIME_DOWNLOAD_ENV = "MESH_S3_BINARY_CACHE_EXPIRATION_TIME_DOWNLOAD";
    public static final String MESH_S3_BINARY_CACHE_REGION_ENV = "MESH_S3_BINARY_CACHE_REGION";
    public static final String MESH_S3_BINARY_CACHE_SECRET_ENV = "MESH_S3_BINARY_CACHE_SECRET";

    @JsonProperty(required = false)
    @JsonPropertyDescription("Used for authentication with Amazon Web Services for cached images (binary transformation variants).")
    @EnvironmentVariable(name = MESH_S3_BINARY_CACHE_AWS_KEY_ENV, description = "Override the configured awskey.")
    private String awsKey;

    @JsonProperty(required = false)
    @JsonPropertyDescription("AWS S3 bucket where transformed/resized images will be uploaded to and downloaded from.")
    @EnvironmentVariable(name = MESH_S3_BINARY_CACHE_BUCKET_ENV, description = "Override the configured AWS S3 bucket.")
    private String bucket;

    @JsonProperty(required = false)
    @JsonPropertyDescription("After this time in milliseconds the AWS S3 URL used for download of a transformed image will expire.")
    @EnvironmentVariable(name = MESH_S3_BINARY_CACHE_EXPIRATION_TIME_DOWNLOAD_ENV, description = "Override the configured AWS S3 download time.")
    private int expirationTimeDownload = DEFAULT_EXPIRATION_TIME_CACHE_DOWNLOAD;

    //As this will only be used internally, we can hardcode a sensible time
    private int expirationTimeUpload = DEFAULT_EXPIRATION_TIME_CACHE_UPLOAD;

    @JsonProperty(required = false)
    @JsonPropertyDescription("AWS S3 region for transformed / resized images")
    @EnvironmentVariable(name = MESH_S3_BINARY_CACHE_REGION_ENV, description = "Override the configured AWS S3 region.")
    private String region;

    @JsonProperty(required = false)
    @JsonPropertyDescription("AWS S3 Cache key")
    @EnvironmentVariable(name = MESH_S3_BINARY_CACHE_SECRET_ENV, description = "Override the configured AWS S3 secret.")
    private String secret;
}
