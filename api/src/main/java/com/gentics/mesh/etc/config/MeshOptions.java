package com.gentics.mesh.etc.config;

import java.util.HashMap;
import java.util.Map;

import com.gentics.mesh.etc.ElasticSearchOptions;
import com.gentics.mesh.etc.StorageOptions;

import io.vertx.ext.mail.MailConfig;

/**
 * Main mesh configuration POJO.
 */
public class MeshOptions {

	public static final boolean ENABLED = true;
	public static final boolean DISABLED = false;
	public static final boolean DEFAULT_CLUSTER_MODE = DISABLED;
	public static final int DEFAULT_PAGE_SIZE = 25;
	public static final String DEFAULT_LANGUAGE = "en";
	public static final String DEFAULT_DIRECTORY_NAME = "graphdb";
	public static final String MESH_SESSION_KEY = "mesh.session";
	
	private boolean clusterMode = DEFAULT_CLUSTER_MODE;

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

	/**
	 * Return the cluster mode flag.
	 * 
	 * @return
	 */
	public boolean isClusterMode() {
		return clusterMode;
	}

	/**
	 * Set the flag which can toggle the cluster mode.
	 * 
	 * @param clusterMode
	 */
	public void setClusterMode(boolean clusterMode) {
		this.clusterMode = clusterMode;
	}

	/**
	 * Return the default language.
	 * 
	 * @return
	 */
	public String getDefaultLanguage() {
		return defaultLanguage;
	}

	/**
	 * Return the default page size.
	 * 
	 * @return
	 */
	public int getDefaultPageSize() {
		return defaultPageSize;
	}

	/**
	 * Return the mesh mail server options.
	 * 
	 * @return
	 */
	public MailConfig getMailServerOptions() {
		return this.mailServerOptions;
	}

	/**
	 * Return the mesh storage options.
	 * 
	 * @return
	 */
	public StorageOptions getStorageOptions() {
		return this.storageOptions;
	}

	/**
	 * Return the mesh upload options.
	 * 
	 * @return
	 */
	public MeshUploadOptions getUploadOptions() {
		return uploadOptions;
	}

	/**
	 * Set the mesh upload options.
	 * 
	 * @param uploadOptions
	 */
	public void setUploadOptions(MeshUploadOptions uploadOptions) {
		this.uploadOptions = uploadOptions;
	}

	/**
	 * Return the http server options.
	 * 
	 * @return
	 */
	public HttpServerConfig getHttpServerOptions() {
		return httpServerOptions;
	}

	/**
	 * Set the http server options.
	 * 
	 * @param httpServerOptions
	 */
	public void setHttpServerOptions(HttpServerConfig httpServerOptions) {
		this.httpServerOptions = httpServerOptions;
	}

	/**
	 * Return the search options.
	 * 
	 * @return
	 */
	public ElasticSearchOptions getSearchOptions() {
		return searchOptions;
	}
}
