package com.gentics.mesh.etc.config;

import io.vertx.ext.mail.MailConfig;

import java.util.HashMap;
import java.util.Map;

import com.gentics.mesh.etc.MeshVerticleConfiguration;
import com.gentics.mesh.etc.StorageOptions;

public class MeshOptions {

	// private static final boolean ENABLED = true;
	private static final boolean DISABLED = false;

	public static final String HTTP_PORT_KEY = "httpPort";
	public static final int DEFAULT_HTTP_PORT = 8080;
	public static final boolean DEFAULT_CLUSTER_MODE = DISABLED;
	public static final int DEFAULT_MAX_DEPTH = 5;
	public static final int DEFAULT_PAGE_SIZE = 25;
	public static final String DEFAULT_LANGUAGE = "en";
	public static final int DEFAULT_NESTED_TAGS_LIMIT = 25;
	public static final int DEFAULT_NESTED_NODES_LIMIT = 25;
	public static final String DEFAULT_DIRECTORY_NAME = "graphdb";
	public static final String DEFAULT_NEO4VERTX_BASE_ADDRESS = "graph";
	public static final String DEFAULT_NEO4VERTX_MODE = "default";
	public static final String DEFAULT_NEO4J_WEB_SERVER_BIND_ADDRESS = "0.0.0.0";
	public static final long DEFAULT_FILEUPLOAD_BYTE_LIMIT = 1024 * 1024 * 250;
	public static final String DEFAULT_DATABASE_PROVIDER_CLASS = "com.gentics.mesh.graphdb.TinkerGraphDatabaseProviderImpl";
	public static final String DEFAULT_CORS_ALLOWED_ORIGIN_PATTERN = "NOT_SET";
	//public static final String DEFAULT_DATABASE_PROVIDER_CLASS = "com.gentics.mesh.graphdb.OrientDBDatabaseProviderImpl";

	private String databaseProviderClass = DEFAULT_DATABASE_PROVIDER_CLASS;

	private int httpPort = DEFAULT_HTTP_PORT;

	private int maxDepth = DEFAULT_MAX_DEPTH;

	private boolean clusterMode = DEFAULT_CLUSTER_MODE;

	// TODO fileupload limit per project?
	private long fileUploadByteLimit = DEFAULT_FILEUPLOAD_BYTE_LIMIT;

	private int defaultNestedTagsLimit = DEFAULT_NESTED_TAGS_LIMIT;

	private int defaultNestedNodesLimit = DEFAULT_NESTED_NODES_LIMIT;

	private int defaultPageSize = DEFAULT_PAGE_SIZE;

	private String defaultLanguage = DEFAULT_LANGUAGE;

	private Map<String, MeshVerticleConfiguration> verticles = new HashMap<>();

	private String corsAllowedOriginPattern = DEFAULT_CORS_ALLOWED_ORIGIN_PATTERN;

	private Boolean enableCors = false;

	private MailConfig mailServerOptions = new MailConfig();

	private StorageOptions storageOptions = new StorageOptions();

	public MeshOptions() {
	}

	public Map<String, MeshVerticleConfiguration> getVerticles() {
		return verticles;
	}

	public boolean isClusterMode() {
		return clusterMode;
	}

	public void setClusterMode(boolean clusterMode) {
		this.clusterMode = clusterMode;
	}

	public int getHttpPort() {
		return httpPort;
	}

	public void setHttpPort(int httpPort) {
		this.httpPort = httpPort;
	}

	public int getMaxDepth() {
		return maxDepth;
	}

	public void setMaxDepth(int maxDepth) {
		this.maxDepth = maxDepth;
	}

	public long getFileUploadByteLimit() {
		return fileUploadByteLimit;
	}

	public String getDefaultLanguage() {
		return defaultLanguage;
	}

	public int getDefaultPageSize() {
		return defaultPageSize;
	}

	public int getDefaultNestedNodesLimit() {
		return defaultNestedNodesLimit;
	}

	public int getDefaultNestedTagsLimit() {
		return defaultNestedTagsLimit;
	}

	public String getDatabaseProviderClass() {
		return databaseProviderClass;
	}

	public void setDatabaseProviderClass(String databaseProviderClass) {
		this.databaseProviderClass = databaseProviderClass;
	}

	public String getCorsAllowedOriginPattern() {
		return this.corsAllowedOriginPattern;
	}

	public void setCorsAllowedOriginPattern(String corsAllowedOriginPattern) {
		this.corsAllowedOriginPattern = corsAllowedOriginPattern;
	}

	public MailConfig getMailServerOptions() {
		return this.mailServerOptions;
	}

	public StorageOptions getStorageOptions() {
		return this.storageOptions;
	}

	public Boolean getEnableCors() {
		return enableCors;
	}

	public void setEnableCors(Boolean enableCors) {
		this.enableCors = enableCors;
	}

	public boolean isCorsEnabled() {
		return this.enableCors != null && this.enableCors == true;
	}

}
