package com.gentics.mesh.etc.config;

import java.io.File;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import com.gentics.mesh.doc.GenerateDocumentation;

/**
 * Main mesh configuration POJO.
 */
@GenerateDocumentation
public class MeshOptions {

	public static final String DEFAULT_LANGUAGE = "en";
	public static final String DEFAULT_DIRECTORY_NAME = "graphdb";
	public static final int DEFAULT_MAX_DEPTH = 10;

	private int defaultMaxDepth = DEFAULT_MAX_DEPTH;

	@JsonProperty(required = false)
	@JsonPropertyDescription("Configure system wide default language. This language is automatically used if no language has been specified within the REST query parameters or GraphQL query arguments.")
	private String defaultLanguage = DEFAULT_LANGUAGE;

	@JsonProperty(required = false)
	@JsonPropertyDescription("Turn on or off the update checker.")
	private boolean updateCheck = true;

	@JsonProperty(required = true)
	@JsonPropertyDescription("Http server options.")
	private HttpServerConfig httpServerOptions = new HttpServerConfig();

	@JsonProperty(required = true)
	@JsonPropertyDescription("Cluster options.")
	private ClusterOptions clusterOptions = new ClusterOptions();

	@JsonProperty(required = true)
	@JsonPropertyDescription("Graph database options.")
	private GraphStorageOptions storageOptions = new GraphStorageOptions();

	@JsonProperty(required = true)
	@JsonPropertyDescription("Search engine options.")
	private ElasticSearchOptions searchOptions = new ElasticSearchOptions();

	@JsonProperty(required = true)
	@JsonPropertyDescription("File upload options.")
	private MeshUploadOptions uploadOptions = new MeshUploadOptions();

	@JsonProperty(required = true)
	@JsonPropertyDescription("Authentication options.")
	private AuthenticationOptions authenticationOptions = new AuthenticationOptions();

	@JsonProperty(required = true)
	@JsonPropertyDescription("Image handling options.")
	private ImageManipulatorOptions imageOptions = new ImageManipulatorOptions();

	@JsonProperty(required = false)
	@JsonPropertyDescription("Path to the central tmp directory.")
	private String tempDirectory = "data" + File.separator + "tmp";

	@JsonProperty(required = false)
	@JsonPropertyDescription("Name of the cluster node instance. If not specified a name will be generated.")
	private String nodeName;

	/* EXTRA Command Line Arguments */
	@JsonIgnore
	private boolean isInitCluster = false;

	public MeshOptions() {
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
	 * @return Fluent API
	 */
	public MeshOptions setDefaultMaxDepth(int defaultMaxDepth) {
		this.defaultMaxDepth = defaultMaxDepth;
		return this;
	}

	/**
	 * Return the mesh graph database storage options.
	 * 
	 * @return Storage options
	 */
	@JsonProperty("storage")
	public GraphStorageOptions getStorageOptions() {
		return this.storageOptions;
	}

	/**
	 * Return the mesh upload options.
	 * 
	 * @return Upload options
	 */
	@JsonProperty("upload")
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
	@JsonProperty("httpServer")
	public HttpServerConfig getHttpServerOptions() {
		return httpServerOptions;
	}

	@JsonProperty("cluster")
	public ClusterOptions getClusterOptions() {
		return clusterOptions;
	}

	public void setClusterOptions(ClusterOptions clusterOptions) {
		this.clusterOptions = clusterOptions;
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
	@JsonProperty("search")
	public ElasticSearchOptions getSearchOptions() {
		return searchOptions;
	}

	/**
	 * Set the search options.
	 * 
	 * @param searchOptions
	 *            Search options
	 * @return Fluent API
	 */
	public MeshOptions setSearchOptions(ElasticSearchOptions searchOptions) {
		this.searchOptions = searchOptions;
		return this;
	}

	/**
	 * Return the authentication options
	 * 
	 * @return Authentication options
	 */
	@JsonProperty("security")
	public AuthenticationOptions getAuthenticationOptions() {
		return authenticationOptions;
	}

	/**
	 * Set the authentication options
	 * 
	 * @param authenticationOptions
	 *            Authentication options
	 * @return Fluent API
	 */
	public MeshOptions setAuthenticationOptions(AuthenticationOptions authenticationOptions) {
		this.authenticationOptions = authenticationOptions;
		return this;
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
	@JsonProperty("image")
	public ImageManipulatorOptions getImageOptions() {
		return imageOptions;
	}

	/**
	 * Set the image manipulation options.
	 * 
	 * @param imageOptions
	 * @return Fluent API
	 */
	public MeshOptions setImageOptions(ImageManipulatorOptions imageOptions) {
		this.imageOptions = imageOptions;
		return this;
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
	 * @return Fluent API
	 */
	public MeshOptions setUpdateCheck(boolean updateCheck) {
		this.updateCheck = updateCheck;
		return this;
	}

	/**
	 * Set the node name.
	 * 
	 * @param nodeName
	 * @return
	 */
	public MeshOptions setNodeName(String nodeName) {
		this.nodeName = nodeName;
		return this;
	}

	/**
	 * Return the node name.
	 * 
	 * @return
	 */
	public String getNodeName() {
		return nodeName;
	}

	@JsonIgnore
	public boolean isInitClusterMode() {
		return isInitCluster;
	}

	@JsonIgnore
	public MeshOptions setInitCluster(boolean isInitCluster) {
		this.isInitCluster = isInitCluster;
		return this;
	}

	public void validate() {
		if (getClusterOptions() != null) {
			getClusterOptions().validate(this);
		}
		if (getStorageOptions() != null) {
			getStorageOptions().validate(this);
		}
		if (getSearchOptions() != null) {
			getSearchOptions().validate(this);
		}
		if (getHttpServerOptions() != null) {
			getHttpServerOptions().validate(this);
		}
		if (getAuthenticationOptions() != null) {
			getAuthenticationOptions().validate(this);
		}
		if (getImageOptions() != null) {
			getImageOptions().validate(this);
		}

		// TODO check for other invalid characters in node name
	}

}