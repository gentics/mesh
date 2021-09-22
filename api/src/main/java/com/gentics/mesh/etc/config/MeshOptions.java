package com.gentics.mesh.etc.config;

import java.io.File;
import java.util.Objects;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import com.gentics.mesh.doc.GenerateDocumentation;
import com.gentics.mesh.etc.config.env.EnvironmentVariable;
import com.gentics.mesh.etc.config.env.Option;
import com.gentics.mesh.etc.config.search.ElasticSearchOptions;
import com.gentics.mesh.util.PasswordUtil;

/**
 * Main mesh configuration POJO.
 */
@GenerateDocumentation
public class MeshOptions implements Option {

	public static final String DEFAULT_LANGUAGE = "en";
	public static final String DEFAULT_DIRECTORY_NAME = "graphdb";
	public static final int DEFAULT_MAX_DEPTH = 10;
	public static final int DEFAULT_PLUGIN_TIMEOUT = 120;

	public static final String MESH_DEFAULT_LANG_ENV = "MESH_DEFAULT_LANG";
	public static final String MESH_LANGUAGES_FILE_PATH_ENV = "MESH_LANGUAGES_FILE_PATH";
	public static final String MESH_UPDATECHECK_ENV = "MESH_UPDATECHECK";
	public static final String MESH_TEMP_DIR_ENV = "MESH_TEMP_DIR";
	public static final String MESH_PLUGIN_DIR_ENV = "MESH_PLUGIN_DIR";
	public static final String MESH_PLUGIN_TIMEOUT_ENV = "MESH_PLUGIN_TIMEOUT";
	public static final String MESH_NODE_NAME_ENV = "MESH_NODE_NAME";
	public static final String MESH_CLUSTER_INIT_ENV = "MESH_CLUSTER_INIT";
	public static final String MESH_LOCK_PATH_ENV = "MESH_LOCK_PATH";
	public static final String MESH_LIVE_PATH_ENV = "MESH_LIVE_PATH";
	public static final String MESH_START_IN_READ_ONLY_ENV = "MESH_START_IN_READ_ONLY";
	public static final String MESH_INITIAL_ADMIN_PASSWORD_ENV = "MESH_INITIAL_ADMIN_PASSWORD";
	public static final String MESH_INITIAL_ADMIN_PASSWORD_FORCE_RESET_ENV = "MESH_INITIAL_ADMIN_PASSWORD_FORCE_RESET";
	public static final String MESH_MAX_PURGE_BATCH_SIZE = "MESH_MAX_PURGE_BATCH_SIZE";

	// TODO remove this setting. There should not be a default max depth. This is no longer needed once we remove the expand all parameter
	private int defaultMaxDepth = DEFAULT_MAX_DEPTH;

	@JsonProperty(required = false)
	@JsonPropertyDescription("Configure system wide default language. This language is automatically used if no language has been specified within the REST query parameters or GraphQL query arguments.")
	@EnvironmentVariable(name = MESH_DEFAULT_LANG_ENV, description = "Override the configured default language.")
	private String defaultLanguage = DEFAULT_LANGUAGE;

	@JsonProperty(required = false)
	@JsonPropertyDescription("Optional path to a JSON file containing additional languages")
	@EnvironmentVariable(name = MESH_LANGUAGES_FILE_PATH_ENV, description = "Override the path to the optional languages file")
	private String languagesFilePath;

	@JsonProperty(required = false)
	@JsonPropertyDescription("Turn on or off the update checker.")
	@EnvironmentVariable(name = MESH_UPDATECHECK_ENV, description = "Override the configured updatecheck flag.")
	private boolean updateCheck = true;

	@JsonProperty(required = true)
	@JsonPropertyDescription("Http server options.")
	private HttpServerConfig httpServerOptions = new HttpServerConfig();

	@JsonProperty(required = true)
	@JsonPropertyDescription("Monitoring options.")
	private MonitoringConfig monitoringOptions = new MonitoringConfig();

	@JsonProperty(required = true)
	@JsonPropertyDescription("Vert.x specific options.")
	private VertxOptions vertxOptions = new VertxOptions();

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
	@JsonPropertyDescription("S3 options.")
	private S3Options s3options = new S3Options();

	@JsonProperty(required = true)
	@JsonPropertyDescription("Authentication options.")
	private AuthenticationOptions authenticationOptions = new AuthenticationOptions();

	@JsonProperty(required = true)
	@JsonPropertyDescription("Image handling options.")
	private ImageManipulatorOptions imageOptions = new ImageManipulatorOptions();

	@JsonProperty(required = true)
	@JsonPropertyDescription("Content related options.")
	private ContentConfig contentOptions = new ContentConfig();

	@JsonProperty(required = true)
	@JsonPropertyDescription("Cache options.")
	private CacheConfig cacheConfig = new CacheConfig();

	@JsonProperty(required = true)
	@JsonPropertyDescription("Debug info options.")
	private DebugInfoOptions debugInfoOptions = new DebugInfoOptions();

	@JsonProperty(required = true)
	@JsonPropertyDescription("GraphQL options.")
	private GraphQLOptions graphQLOptions = new GraphQLOptions();

	@JsonProperty(required = false)
	@JsonPropertyDescription("Path to the central tmp directory.")
	@EnvironmentVariable(name = MESH_TEMP_DIR_ENV, description = "Override the configured temp directory.")
	private String tempDirectory = "data" + File.separator + "tmp";

	@JsonProperty(required = false)
	@JsonPropertyDescription("Path to the plugin directory.")
	@EnvironmentVariable(name = MESH_PLUGIN_DIR_ENV, description = "Override the configured plugin directory.")
	private String pluginDirectory = "plugins";

	@JsonProperty(required = false)
	@JsonPropertyDescription("Timeout in seconds which is used for the plugin startup,initialization,de-initialization and stop processes. Default: "
		+ DEFAULT_PLUGIN_TIMEOUT + " seconds.")
	@EnvironmentVariable(name = MESH_PLUGIN_TIMEOUT_ENV, description = "Override the configured plugin timeout.")
	private int pluginTimeout = DEFAULT_PLUGIN_TIMEOUT;

	@JsonProperty(required = false)
	@JsonPropertyDescription("Name of the cluster node instance. If not specified a name will be generated.")
	@EnvironmentVariable(name = MESH_NODE_NAME_ENV, description = "Override the configured node name.")
	private String nodeName;

	@JsonProperty(required = false)
	@JsonPropertyDescription("If true, Gentics Mesh will be started in read only mode.")
	@EnvironmentVariable(name = MESH_START_IN_READ_ONLY_ENV, description = "Override the read only mode flag.")
	private boolean startInReadOnly = false;

	@JsonProperty(required = false)
	@JsonPropertyDescription("The maximum amount of node versions that are purged before the database transaction is committed.")
	@EnvironmentVariable(name = MESH_MAX_PURGE_BATCH_SIZE, description = "Override the maximum purge batch size.")
	private int versionPurgeMaxBatchSize = 10;

	/* EXTRA Command Line Arguments */
	@JsonIgnore
	@EnvironmentVariable(name = MESH_CLUSTER_INIT_ENV, description = "Enable or disable the initial cluster database setup. This is useful for testing.")
	private boolean isInitCluster = false;

	@JsonIgnore
	@EnvironmentVariable(name = MESH_LOCK_PATH_ENV, description = "Path to the mesh lock file.")
	private String lockPath = "mesh.lock";

	@JsonIgnore
	@EnvironmentVariable(name = MESH_LIVE_PATH_ENV, description = "Path to the mesh live file.")
	private String livePath = "mesh.live";

	@JsonIgnore
	@EnvironmentVariable(name = MESH_INITIAL_ADMIN_PASSWORD_ENV, description = "Password which will be used during initial admin user creation.")
	private String initialAdminPassword = PasswordUtil.humanPassword();

	@JsonIgnore
	@EnvironmentVariable(name = MESH_INITIAL_ADMIN_PASSWORD_FORCE_RESET_ENV, description = "Control whether a forced password reset should be triggered when creating the initial admin user. Default: true")
	private boolean forceInitialAdminPasswordReset = true;

	@JsonIgnore
	private String adminPassword;

	public String getDefaultLanguage() {
		return defaultLanguage;
	}

	public MeshOptions setDefaultLanguage(String defaultLanguage) {
		this.defaultLanguage = defaultLanguage;
		return this;
	}

	public String getLanguagesFilePath() {
		return languagesFilePath;
	}

	public MeshOptions setLanguagesFilePath(String languagesFilePath) {
		this.languagesFilePath = languagesFilePath;
		return this;
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

	@JsonProperty("storage")
	public GraphStorageOptions getStorageOptions() {
		return this.storageOptions;
	}

	public MeshOptions setStorageOptions(GraphStorageOptions storageOptions) {
		this.storageOptions = storageOptions;
		return this;
	}

	@JsonProperty("s3options")
	public S3Options getS3Options() {
		if (s3options == null) {
			s3options = new S3Options();
		}
		return s3options;
	}

	public MeshOptions setS3Options(S3Options s3options) {
		this.s3options = s3options;
		return this;
	}

	@JsonProperty("upload")
	public MeshUploadOptions getUploadOptions() {
		return uploadOptions;
	}

	public MeshOptions setUploadOptions(MeshUploadOptions uploadOptions) {
		this.uploadOptions = uploadOptions;
		return this;
	}

	@JsonProperty("httpServer")
	public HttpServerConfig getHttpServerOptions() {
		return httpServerOptions;
	}

	public MeshOptions setHttpServerOptions(HttpServerConfig httpServerOptions) {
		this.httpServerOptions = httpServerOptions;
		return this;
	}

	@JsonProperty("monitoring")
	public MonitoringConfig getMonitoringOptions() {
		return monitoringOptions;
	}

	public MeshOptions setMonitoringOptions(MonitoringConfig monitoringOptions) {
		this.monitoringOptions = monitoringOptions;
		return this;
	}

	public VertxOptions getVertxOptions() {
		return vertxOptions;
	}

	public MeshOptions setVertxOptions(VertxOptions vertxOptions) {
		this.vertxOptions = vertxOptions;
		return this;
	}

	@JsonProperty("cluster")
	public ClusterOptions getClusterOptions() {
		return clusterOptions;
	}

	public MeshOptions setClusterOptions(ClusterOptions clusterOptions) {
		this.clusterOptions = clusterOptions;
		return this;
	}

	@JsonProperty("search")
	public ElasticSearchOptions getSearchOptions() {
		return searchOptions;
	}

	public MeshOptions setSearchOptions(ElasticSearchOptions searchOptions) {
		this.searchOptions = searchOptions;
		return this;
	}

	@JsonProperty("security")
	public AuthenticationOptions getAuthenticationOptions() {
		return authenticationOptions;
	}

	public MeshOptions setAuthenticationOptions(AuthenticationOptions authenticationOptions) {
		this.authenticationOptions = authenticationOptions;
		return this;
	}

	public String getTempDirectory() {
		return tempDirectory;
	}

	public MeshOptions setTempDirectory(String tempDirectory) {
		this.tempDirectory = tempDirectory;
		return this;
	}

	public String getPluginDirectory() {
		return pluginDirectory;
	}

	public MeshOptions setPluginDirectory(String pluginDirectory) {
		this.pluginDirectory = pluginDirectory;
		return this;
	}

	@JsonProperty("image")
	public ImageManipulatorOptions getImageOptions() {
		return imageOptions;
	}

	public MeshOptions setImageOptions(ImageManipulatorOptions imageOptions) {
		this.imageOptions = imageOptions;
		return this;
	}

	@JsonProperty("content")
	public ContentConfig getContentOptions() {
		return contentOptions;
	}

	public MeshOptions setContentOptions(ContentConfig contentOptions) {
		this.contentOptions = contentOptions;
		return this;
	}

	@JsonProperty("debugInfo")
	public DebugInfoOptions getDebugInfoOptions() {
		return debugInfoOptions;
	}

	public MeshOptions setDebugInfoOptions(DebugInfoOptions debugInfoOptions) {
		this.debugInfoOptions = debugInfoOptions;
		return this;
	}

	@JsonProperty("cache")
	public CacheConfig getCacheConfig() {
		return cacheConfig;
	}

	/**
	 * Get the graphql options
	 * @return graphql options
	 */
	@JsonProperty("graphQL")
	public GraphQLOptions getGraphQLOptions() {
		return graphQLOptions;
	}

	/**
	 * Set the graphql options
	 * @param graphQlOptions options
	 * @return fluent API
	 */
	public MeshOptions setGraphQlOptions(GraphQLOptions graphQlOptions) {
		this.graphQLOptions = graphQlOptions;
		return this;
	}

	public MeshOptions setCacheConfig(CacheConfig cacheConfig) {
		this.cacheConfig = cacheConfig;
		return this;
	}

	@JsonProperty("updateCheck")
	public boolean isUpdateCheckEnabled() {
		return updateCheck;
	}

	public MeshOptions setUpdateCheck(boolean updateCheck) {
		this.updateCheck = updateCheck;
		return this;
	}

	public String getNodeName() {
		return nodeName;
	}

	public MeshOptions setNodeName(String nodeName) {
		this.nodeName = nodeName;
		return this;
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

	@JsonIgnore
	public String getLockPath() {
		return lockPath;
	}

	@JsonIgnore
	public MeshOptions setLockPath(String lockPath) {
		this.lockPath = lockPath;
		return this;
	}

	@JsonIgnore
	public String getLivePath() {
		return livePath;
	}

	@JsonIgnore
	public MeshOptions setLivePath(String livePath) {
		this.livePath = livePath;
		return this;
	}

	@JsonIgnore
	public String getAdminPassword() {
		return adminPassword;
	}

	@JsonIgnore
	public MeshOptions setAdminPassword(String adminPassword) {
		this.adminPassword = adminPassword;
		return this;
	}

	@JsonIgnore
	public String getInitialAdminPassword() {
		return initialAdminPassword;
	}

	@JsonIgnore
	public MeshOptions setInitialAdminPassword(String initialAdminPassword) {
		this.initialAdminPassword = initialAdminPassword;
		return this;
	}

	@JsonIgnore
	public boolean isForceInitialAdminPasswordReset() {
		return forceInitialAdminPasswordReset;
	}

	@JsonIgnore
	public MeshOptions setForceInitialAdminPasswordReset(boolean forceInitialAdminPasswordReset) {
		this.forceInitialAdminPasswordReset = forceInitialAdminPasswordReset;
		return this;
	}

	public int getPluginTimeout() {
		return pluginTimeout;
	}

	public MeshOptions setPluginTimeout(int pluginTimeout) {
		this.pluginTimeout = pluginTimeout;
		return this;
	}

	public boolean isStartInReadOnly() {
		return startInReadOnly;
	}

	public MeshOptions setStartInReadOnly(boolean startInReadOnly) {
		this.startInReadOnly = startInReadOnly;
		return this;
	}

	public int getVersionPurgeMaxBatchSize() {
		return versionPurgeMaxBatchSize;
	}

	public MeshOptions setVersionPurgeMaxBatchSize(int versionPurgeMaxBatchSize) {
		this.versionPurgeMaxBatchSize = versionPurgeMaxBatchSize;
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
		if (getMonitoringOptions() != null) {
			getMonitoringOptions().validate(this);
		}
		if (getContentOptions() != null) {
			getContentOptions().validate(this);
		}
		if (getGraphQLOptions() != null) {
			getGraphQLOptions().validate(this);
		}
		if (getS3Options() != null) {
			getS3Options().validate(this);
		}
		Objects.requireNonNull(getNodeName(), "The node name must be specified.");
		if (getVersionPurgeMaxBatchSize() <= 0) {
			throw new IllegalArgumentException("versionPurgeMaxBatchSize must be positive.");
		}
		// TODO check for other invalid characters in node name
	}

	@Override
	public void validate(MeshOptions options) {
		validate();
	}

}
