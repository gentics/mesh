package com.gentics.mesh.etc.config;

import java.io.File;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.gentics.mesh.etc.ElasticSearchOptions;
import com.gentics.mesh.etc.GraphStorageOptions;

/**
 * Main mesh configuration POJO.
 */
public class MeshOptions {

	public static final boolean ENABLED = true;
	public static final boolean DISABLED = false;
	public static final boolean DEFAULT_CLUSTER_MODE = DISABLED;
	public static final String DEFAULT_LANGUAGE = "en";
	public static final String DEFAULT_DIRECTORY_NAME = "graphdb";
	public static final int DEFAULT_MAX_DEPTH = 10;

	private boolean clusterMode = DEFAULT_CLUSTER_MODE;

	private int defaultMaxDepth = DEFAULT_MAX_DEPTH;

	private String defaultLanguage = DEFAULT_LANGUAGE;

	private boolean updateCheck = ENABLED;

	private HttpServerConfig httpServerOptions = new HttpServerConfig();

	private GraphStorageOptions storageOptions = new GraphStorageOptions();

	private ElasticSearchOptions searchOptions = new ElasticSearchOptions();

	private MeshUploadOptions uploadOptions = new MeshUploadOptions();

	private AuthenticationOptions authenticationOptions = new AuthenticationOptions();

	private ImageManipulatorOptions imageOptions = new ImageManipulatorOptions();

	private String tempDirectory = "data" + File.separator + "tmp";

	public MeshOptions() {
	}

	/**
	 * Return the cluster mode flag.
	 * 
	 * @return Flag value
	 */
	public boolean isClusterMode() {
		return clusterMode;
	}

	/**
	 * Set the flag which can toggle the cluster mode.
	 * 
	 * @param clusterMode
	 *            Flag value
	 */
	public void setClusterMode(boolean clusterMode) {
		this.clusterMode = clusterMode;
	}

	/**
	 * Return the default language.
	 * 
	 * @return Language tag of the default language
	 */
	public String getDefaultLanguage() {
		return defaultLanguage;
	}

	/**
	 * Return the default max depth for navigations.
	 * 
	 * @return
	 */
	public int getDefaultMaxDepth() {
		return defaultMaxDepth;
	}

	/**
	 * Set the default max depth for navigations.
	 * 
	 * @param defaultMaxDepth
	 */
	public void setDefaultMaxDepth(int defaultMaxDepth) {
		this.defaultMaxDepth = defaultMaxDepth;
	}

	/**
	 * Return the mesh graph database storage options.
	 * 
	 * @return Storage options
	 */
	public GraphStorageOptions getStorageOptions() {
		return this.storageOptions;
	}

	/**
	 * Return the mesh upload options.
	 * 
	 * @return Upload options
	 */
	public MeshUploadOptions getUploadOptions() {
		return uploadOptions;
	}

	/**
	 * Set the mesh upload options.
	 * 
	 * @param uploadOptions
	 *            Upload options
	 */
	public void setUploadOptions(MeshUploadOptions uploadOptions) {
		this.uploadOptions = uploadOptions;
	}

	/**
	 * Return the http server options.
	 * 
	 * @return Http server options
	 */
	public HttpServerConfig getHttpServerOptions() {
		return httpServerOptions;
	}

	/**
	 * Set the http server options.
	 * 
	 * @param httpServerOptions
	 *            Http server options
	 */
	public void setHttpServerOptions(HttpServerConfig httpServerOptions) {
		this.httpServerOptions = httpServerOptions;
	}

	/**
	 * Return the search options.
	 * 
	 * @return Search options
	 */
	public ElasticSearchOptions getSearchOptions() {
		return searchOptions;
	}

	/**
	 * Set the search options.
	 * 
	 * @param searchOptions
	 *            Search options
	 */
	public void setSearchOptions(ElasticSearchOptions searchOptions) {
		this.searchOptions = searchOptions;
	}

	/**
	 * Return the authentication options
	 * 
	 * @return Authentication options
	 */
	public AuthenticationOptions getAuthenticationOptions() {
		return authenticationOptions;
	}

	/**
	 * Set the authentication options
	 * 
	 * @param authenticationOptions
	 *            Authentication options
	 */
	public void setAuthenticationOptions(AuthenticationOptions authenticationOptions) {
		this.authenticationOptions = authenticationOptions;
	}

	/**
	 * Returns the temporary directory.
	 * 
	 * @return Temporary filesystem directory
	 */
	public String getTempDirectory() {
		return tempDirectory;
	}

	/**
	 * Set the temporary directory.
	 * 
	 * @param tempDirectory
	 *            Temporary filesystem directory
	 */
	public void setTempDirectory(String tempDirectory) {
		this.tempDirectory = tempDirectory;
	}

	/**
	 * Return the image manipulation options.
	 * 
	 * @return
	 */
	public ImageManipulatorOptions getImageOptions() {
		return imageOptions;
	}

	/**
	 * Set the image manipulation options.
	 * 
	 * @param imageOptions
	 */
	public void setImageOptions(ImageManipulatorOptions imageOptions) {
		this.imageOptions = imageOptions;
	}

	/**
	 * Return update checker flag.
	 * 
	 * @return
	 */
	@JsonProperty("updateCheck")
	public boolean isUpdateCheckEnabled() {
		return updateCheck;
	}

	/**
	 * Set the update checker flag. If set to true a update check will be invoked during mesh server startup.
	 * 
	 * @param updateCheck
	 */
	public void setUpdateCheck(boolean updateCheck) {
		this.updateCheck = updateCheck;
	}

}
