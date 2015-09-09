package com.gentics.mesh.etc.config;

import java.util.HashMap;
import java.util.Map;

import com.gentics.mesh.etc.ElasticSearchOptions;
import com.gentics.mesh.etc.MeshVerticleConfiguration;
import com.gentics.mesh.etc.StorageOptions;

import io.vertx.ext.mail.MailConfig;

public class MeshOptions {

	public static final boolean ENABLED = true;
	public static final boolean DISABLED = false;
	public static final boolean DEFAULT_CLUSTER_MODE = DISABLED;
	public static final int DEFAULT_MAX_DEPTH = 5;
	public static final int DEFAULT_PAGE_SIZE = 25;
	public static final String DEFAULT_LANGUAGE = "en";
	public static final int DEFAULT_NESTED_TAGS_LIMIT = 25;
	public static final int DEFAULT_NESTED_NODES_LIMIT = 25;
	public static final String DEFAULT_DIRECTORY_NAME = "graphdb";

	private int maxDepth = DEFAULT_MAX_DEPTH;

	private boolean clusterMode = DEFAULT_CLUSTER_MODE;

	private int defaultNestedTagsLimit = DEFAULT_NESTED_TAGS_LIMIT;

	private int defaultNestedNodesLimit = DEFAULT_NESTED_NODES_LIMIT;

	private int defaultPageSize = DEFAULT_PAGE_SIZE;

	private String defaultLanguage = DEFAULT_LANGUAGE;

	private Map<String, MeshVerticleConfiguration> verticles = new HashMap<>();

	private MailConfig mailServerOptions = new MailConfig();

	private HttpServerConfig httpServerOptions = new HttpServerConfig();

	private StorageOptions storageOptions = new StorageOptions();

	private ElasticSearchOptions searchOptions = new ElasticSearchOptions();

	private MeshUploadOptions uploadOptions = new MeshUploadOptions();

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

	public int getMaxDepth() {
		return maxDepth;
	}

	public void setMaxDepth(int maxDepth) {
		this.maxDepth = maxDepth;
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

	public MailConfig getMailServerOptions() {
		return this.mailServerOptions;
	}

	public StorageOptions getStorageOptions() {
		return this.storageOptions;
	}

	public MeshUploadOptions getUploadOptions() {
		return uploadOptions;
	}

	public void setUploadOptions(MeshUploadOptions uploadOptions) {
		this.uploadOptions = uploadOptions;
	}

	public HttpServerConfig getHttpServerOptions() {
		return httpServerOptions;
	}

	public void setHttpServerOptions(HttpServerConfig httpServerOptions) {
		this.httpServerOptions = httpServerOptions;
	}

	public ElasticSearchOptions getSearchOptions() {
		return searchOptions;
	}
}
