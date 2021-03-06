package com.syncleus.ferma.ext.orientdb3;

import com.gentics.mesh.annotation.Setter;
import com.gentics.mesh.cli.BootstrapInitializer;
import com.gentics.mesh.core.data.HibMeshVersion;
import com.gentics.mesh.core.data.dao.PermissionRoots;
import com.gentics.mesh.core.db.TxData;
import com.gentics.mesh.etc.config.*;
import com.gentics.mesh.etc.config.search.ElasticSearchOptions;

/**
 * @see TxData
 */
public class TxDataImpl implements TxData {

	private final BootstrapInitializer boot;
	private final MeshOptions options;
	private final PermissionRoots permissionRoots;

	public TxDataImpl(MeshOptions options, BootstrapInitializer boot, PermissionRoots permissionRoots) {
		this.options = options;
		this.boot = boot;
		this.permissionRoots = permissionRoots;
	}

	@Override
	public MeshOptions options() {
		return options;
	}

	/**
	 * @see MeshOptions#overrideWithEnv()
	 */
	public void overrideWithEnv() {
		options.overrideWithEnv();
	}

	/**
	 * @see MeshOptions#hashCode()
	 */
	public int hashCode() {
		return options.hashCode();
	}

	/**
	 * @see MeshOptions#equals(Object)
	 */
	public boolean equals(Object obj) {
		return options.equals(obj);
	}

	public String getDefaultLanguage() {
		return options.getDefaultLanguage();
	}

	@Setter
	public MeshOptions setDefaultLanguage(String defaultLanguage) {
		return options.setDefaultLanguage(defaultLanguage);
	}

	public String getLanguagesFilePath() {
		return options.getLanguagesFilePath();
	}

	@Setter
	public MeshOptions setLanguagesFilePath(String languagesFilePath) {
		return options.setLanguagesFilePath(languagesFilePath);
	}

	public int getDefaultMaxDepth() {
		return options.getDefaultMaxDepth();
	}

	@Setter
	public MeshOptions setDefaultMaxDepth(int defaultMaxDepth) {
		return options.setDefaultMaxDepth(defaultMaxDepth);
	}

	public GraphStorageOptions getStorageOptions() {
		return options.getStorageOptions();
	}

	@Setter
	public MeshOptions setStorageOptions(GraphStorageOptions storageOptions) {
		return options.setStorageOptions(storageOptions);
	}

	public MeshUploadOptions getUploadOptions() {
		return options.getUploadOptions();
	}

	@Setter
	public MeshOptions setUploadOptions(MeshUploadOptions uploadOptions) {
		return options.setUploadOptions(uploadOptions);
	}

	public HttpServerConfig getHttpServerOptions() {
		return options.getHttpServerOptions();
	}

	@Setter
	public MeshOptions setHttpServerOptions(HttpServerConfig httpServerOptions) {
		return options.setHttpServerOptions(httpServerOptions);
	}

	public MonitoringConfig getMonitoringOptions() {
		return options.getMonitoringOptions();
	}

	@Setter
	public MeshOptions setMonitoringOptions(MonitoringConfig monitoringOptions) {
		return options.setMonitoringOptions(monitoringOptions);
	}

	public VertxOptions getVertxOptions() {
		return options.getVertxOptions();
	}

	@Setter
	public MeshOptions setVertxOptions(VertxOptions vertxOptions) {
		return options.setVertxOptions(vertxOptions);
	}

	public ClusterOptions getClusterOptions() {
		return options.getClusterOptions();
	}

	@Setter
	public MeshOptions setClusterOptions(ClusterOptions clusterOptions) {
		return options.setClusterOptions(clusterOptions);
	}

	public ElasticSearchOptions getSearchOptions() {
		return options.getSearchOptions();
	}

	@Setter
	public MeshOptions setSearchOptions(ElasticSearchOptions searchOptions) {
		return options.setSearchOptions(searchOptions);
	}

	public AuthenticationOptions getAuthenticationOptions() {
		return options.getAuthenticationOptions();
	}

	@Setter
	public MeshOptions setAuthenticationOptions(AuthenticationOptions authenticationOptions) {
		return options.setAuthenticationOptions(authenticationOptions);
	}

	public String getTempDirectory() {
		return options.getTempDirectory();
	}

	@Setter
	public MeshOptions setTempDirectory(String tempDirectory) {
		return options.setTempDirectory(tempDirectory);
	}

	/**
	 * @see MeshOptions#toString()
	 */
	public String toString() {
		return options.toString();
	}

	public String getPluginDirectory() {
		return options.getPluginDirectory();
	}

	@Setter
	public MeshOptions setPluginDirectory(String pluginDirectory) {
		return options.setPluginDirectory(pluginDirectory);
	}

	public S3Options getS3Options() {
		return options.getS3Options();
	}

	@Setter
	public MeshOptions setS3Options(S3Options s3Options) {
		return options.setS3Options(s3Options);
	}

	public S3CacheOptions getS3CacheOptions() {
		return options.getS3Options().getS3CacheOptions();
	}

	@Setter
	public S3Options setS3CacheOptions(S3CacheOptions s3cacheOptions) {
		return options.getS3Options().setS3CacheOptions(s3cacheOptions);
	}

	public ImageManipulatorOptions getImageOptions() {
		return options.getImageOptions();
	}

	@Setter
	public MeshOptions setImageOptions(ImageManipulatorOptions imageOptions) {
		return options.setImageOptions(imageOptions);
	}

	public ContentConfig getContentOptions() {
		return options.getContentOptions();
	}

	@Setter
	public MeshOptions setContentOptions(ContentConfig contentOptions) {
		return options.setContentOptions(contentOptions);
	}

	public DebugInfoOptions getDebugInfoOptions() {
		return options.getDebugInfoOptions();
	}

	@Setter
	public MeshOptions setDebugInfoOptions(DebugInfoOptions debugInfoOptions) {
		return options.setDebugInfoOptions(debugInfoOptions);
	}

	public CacheConfig getCacheConfig() {
		return options.getCacheConfig();
	}

	@Setter
	public MeshOptions setCacheConfig(CacheConfig cacheConfig) {
		return options.setCacheConfig(cacheConfig);
	}

	public boolean isUpdateCheckEnabled() {
		return options.isUpdateCheckEnabled();
	}

	@Setter
	public MeshOptions setUpdateCheck(boolean updateCheck) {
		return options.setUpdateCheck(updateCheck);
	}

	public String getNodeName() {
		return options.getNodeName();
	}

	@Setter
	public MeshOptions setNodeName(String nodeName) {
		return options.setNodeName(nodeName);
	}

	public boolean isInitClusterMode() {
		return options.isInitClusterMode();
	}

	@Setter
	public MeshOptions setInitCluster(boolean isInitCluster) {
		return options.setInitCluster(isInitCluster);
	}

	public String getLockPath() {
		return options.getLockPath();
	}

	@Setter
	public MeshOptions setLockPath(String lockPath) {
		return options.setLockPath(lockPath);
	}

	public String getAdminPassword() {
		return options.getAdminPassword();
	}

	@Setter
	public MeshOptions setAdminPassword(String adminPassword) {
		return options.setAdminPassword(adminPassword);
	}

	public String getInitialAdminPassword() {
		return options.getInitialAdminPassword();
	}

	@Setter
	public MeshOptions setInitialAdminPassword(String initialAdminPassword) {
		return options.setInitialAdminPassword(initialAdminPassword);
	}

	public boolean isForceInitialAdminPasswordReset() {
		return options.isForceInitialAdminPasswordReset();
	}

	@Setter
	public MeshOptions setForceInitialAdminPasswordReset(boolean forceInitialAdminPasswordReset) {
		return options.setForceInitialAdminPasswordReset(forceInitialAdminPasswordReset);
	}

	public int getPluginTimeout() {
		return options.getPluginTimeout();
	}

	@Setter
	public MeshOptions setPluginTimeout(int pluginTimeout) {
		return options.setPluginTimeout(pluginTimeout);
	}

	public boolean isStartInReadOnly() {
		return options.isStartInReadOnly();
	}

	@Setter
	public MeshOptions setStartInReadOnly(boolean startInReadOnly) {
		return options.setStartInReadOnly(startInReadOnly);
	}

	public int getVersionPurgeMaxBatchSize() {
		return options.getVersionPurgeMaxBatchSize();
	}

	@Setter
	public MeshOptions setVersionPurgeMaxBatchSize(int versionPurgeMaxBatchSize) {
		return options.setVersionPurgeMaxBatchSize(versionPurgeMaxBatchSize);
	}

	/**
	 * @see MeshOptions#validate()
	 */
	public void validate() {
		options.validate();
	}

	/**
	 * @see MeshOptions#validate(MeshOptions)
	 */
	public void validate(MeshOptions options) {
		options.validate(options);
	}

	@Override
	public HibMeshVersion meshVersion() {
		return boot.meshRoot();
	}

	@Override
	public PermissionRoots permissionRoots() {
		return permissionRoots;
	}

}
