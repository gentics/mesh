package com.gentics.mesh.etc.config;

import java.util.HashMap;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.gentics.mesh.etc.MeshVerticleConfiguration;

public class MeshConfiguration {

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
	public static final String DEFAULT_DATABASE_PROVIDER_CLASS = "com.gentics.mesh.graphdb.OrientDBDatabaseProviderImpl";

	@JsonProperty("database_provider_class")
	private String databaseProviderClass = DEFAULT_DATABASE_PROVIDER_CLASS;

	@JsonProperty("http_port")
	private int httpPort = DEFAULT_HTTP_PORT;

	@JsonProperty("max_depth")
	private int maxDepth = DEFAULT_MAX_DEPTH;

	@JsonProperty("cluster_mode")
	private boolean clusterMode = DEFAULT_CLUSTER_MODE;

	// TODO fileupload limit per project?
	@JsonProperty("fileupload_byte_limit")
	private long fileUploadByteLimit = DEFAULT_FILEUPLOAD_BYTE_LIMIT;

	@JsonProperty("default_nested_tags_limit")
	private int defaultNestedTagsLimit = DEFAULT_NESTED_TAGS_LIMIT;

	@JsonProperty("default_nested_nodes_limit")
	private int defaultNestedNodesLimit = DEFAULT_NESTED_NODES_LIMIT;

	@JsonProperty("default_page_size")
	private int defaultPageSize = DEFAULT_PAGE_SIZE;

	@JsonProperty("default_language")
	private String defaultLanguage = DEFAULT_LANGUAGE;

	@JsonProperty("verticles")
	private Map<String, MeshVerticleConfiguration> verticles = new HashMap<>();

	public MeshConfiguration() {

		// Check for target directory and use it as a subdirectory if possible
		//		File targetDir = new File("target");
		//		if (targetDir.exists()) {
		//			neo4jConfiguration.setPath(new File(targetDir, DEFAULT_DIRECTORY_NAME).getAbsolutePath());
		//		} else {
		//			neo4jConfiguration.setPath(new File(DEFAULT_DIRECTORY_NAME).getAbsolutePath());
		//		}
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

}
