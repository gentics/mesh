package com.syncleus.ferma.ext.orientdb3;

import com.gentics.mesh.annotation.Setter;
import com.gentics.mesh.cli.BootstrapInitializer;
import com.gentics.mesh.core.data.HibMeshVersion;
import com.gentics.mesh.core.data.dao.PermissionRoots;
import com.gentics.mesh.core.db.TxData;
import com.gentics.mesh.etc.config.AbstractMeshOptions;
import com.gentics.mesh.etc.config.AuthenticationOptions;
import com.gentics.mesh.etc.config.CacheConfig;
import com.gentics.mesh.etc.config.ClusterOptions;
import com.gentics.mesh.etc.config.ContentConfig;
import com.gentics.mesh.etc.config.DebugInfoOptions;
import com.gentics.mesh.etc.config.GraphStorageOptions;
import com.gentics.mesh.etc.config.HttpServerConfig;
import com.gentics.mesh.etc.config.ImageManipulatorOptions;
import com.gentics.mesh.etc.config.MeshOptions;
import com.gentics.mesh.etc.config.MeshUploadOptions;
import com.gentics.mesh.etc.config.MonitoringConfig;
import com.gentics.mesh.etc.config.VertxOptions;
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
	 * @see AbstractMeshOptions#overrideWithEnv()
	 */
	public void overrideWithEnv() {
		options.overrideWithEnv();
	}

	/**
	 * @see AbstractMeshOptions#hashCode()
	 */
	public int hashCode() {
		return options.hashCode();
	}

	/**
	 * @see AbstractMeshOptions#equals(Object)
	 */
	public boolean equals(Object obj) {
		return options.equals(obj);
	}

	public String getDefaultLanguage() {
		return options.getDefaultLanguage();
	}

	@Setter
	public AbstractMeshOptions setDefaultLanguage(String defaultLanguage) {
		return options.setDefaultLanguage(defaultLanguage);
	}

	public String getLanguagesFilePath() {
		return options.getLanguagesFilePath();
	}

	@Setter
	public AbstractMeshOptions setLanguagesFilePath(String languagesFilePath) {
		return options.setLanguagesFilePath(languagesFilePath);
	}

	public int getDefaultMaxDepth() {
		return options.getDefaultMaxDepth();
	}

	@Setter
	public AbstractMeshOptions setDefaultMaxDepth(int defaultMaxDepth) {
		return options.setDefaultMaxDepth(defaultMaxDepth);
	}

	public GraphStorageOptions getStorageOptions() {
		return options.getStorageOptions();
	}

	@Setter
	public AbstractMeshOptions setStorageOptions(GraphStorageOptions storageOptions) {
		return options.setStorageOptions(storageOptions);
	}

	public MeshUploadOptions getUploadOptions() {
		return options.getUploadOptions();
	}

	@Setter
	public AbstractMeshOptions setUploadOptions(MeshUploadOptions uploadOptions) {
		return options.setUploadOptions(uploadOptions);
	}

	public HttpServerConfig getHttpServerOptions() {
		return options.getHttpServerOptions();
	}

	@Setter
	public AbstractMeshOptions setHttpServerOptions(HttpServerConfig httpServerOptions) {
		return options.setHttpServerOptions(httpServerOptions);
	}

	public MonitoringConfig getMonitoringOptions() {
		return options.getMonitoringOptions();
	}

	@Setter
	public AbstractMeshOptions setMonitoringOptions(MonitoringConfig monitoringOptions) {
		return options.setMonitoringOptions(monitoringOptions);
	}

	public VertxOptions getVertxOptions() {
		return options.getVertxOptions();
	}

	@Setter
	public AbstractMeshOptions setVertxOptions(VertxOptions vertxOptions) {
		return options.setVertxOptions(vertxOptions);
	}

	public ClusterOptions getClusterOptions() {
		return options.getClusterOptions();
	}

	@Setter
	public AbstractMeshOptions setClusterOptions(ClusterOptions clusterOptions) {
		return options.setClusterOptions(clusterOptions);
	}

	public ElasticSearchOptions getSearchOptions() {
		return options.getSearchOptions();
	}

	@Setter
	public AbstractMeshOptions setSearchOptions(ElasticSearchOptions searchOptions) {
		return options.setSearchOptions(searchOptions);
	}

	public AuthenticationOptions getAuthenticationOptions() {
		return options.getAuthenticationOptions();
	}

	@Setter
	public AbstractMeshOptions setAuthenticationOptions(AuthenticationOptions authenticationOptions) {
		return options.setAuthenticationOptions(authenticationOptions);
	}

	public String getTempDirectory() {
		return options.getTempDirectory();
	}

	@Setter
	public AbstractMeshOptions setTempDirectory(String tempDirectory) {
		return options.setTempDirectory(tempDirectory);
	}

	/**
	 * @see AbstractMeshOptions#toString()
	 */
	public String toString() {
		return options.toString();
	}

	public String getPluginDirectory() {
		return options.getPluginDirectory();
	}

	@Setter
	public AbstractMeshOptions setPluginDirectory(String pluginDirectory) {
		return options.setPluginDirectory(pluginDirectory);
	}

	public ImageManipulatorOptions getImageOptions() {
		return options.getImageOptions();
	}

	@Setter
	public AbstractMeshOptions setImageOptions(ImageManipulatorOptions imageOptions) {
		return options.setImageOptions(imageOptions);
	}

	public ContentConfig getContentOptions() {
		return options.getContentOptions();
	}

	@Setter
	public AbstractMeshOptions setContentOptions(ContentConfig contentOptions) {
		return options.setContentOptions(contentOptions);
	}

	public DebugInfoOptions getDebugInfoOptions() {
		return options.getDebugInfoOptions();
	}

	@Setter
	public AbstractMeshOptions setDebugInfoOptions(DebugInfoOptions debugInfoOptions) {
		return options.setDebugInfoOptions(debugInfoOptions);
	}

	public CacheConfig getCacheConfig() {
		return options.getCacheConfig();
	}

	@Setter
	public AbstractMeshOptions setCacheConfig(CacheConfig cacheConfig) {
		return options.setCacheConfig(cacheConfig);
	}

	public boolean isUpdateCheckEnabled() {
		return options.isUpdateCheckEnabled();
	}

	@Setter
	public AbstractMeshOptions setUpdateCheck(boolean updateCheck) {
		return options.setUpdateCheck(updateCheck);
	}

	public String getNodeName() {
		return options.getNodeName();
	}

	@Setter
	public AbstractMeshOptions setNodeName(String nodeName) {
		return options.setNodeName(nodeName);
	}

	public boolean isInitClusterMode() {
		return options.isInitClusterMode();
	}

	@Setter
	public AbstractMeshOptions setInitCluster(boolean isInitCluster) {
		return options.setInitCluster(isInitCluster);
	}

	public String getLockPath() {
		return options.getLockPath();
	}

	@Setter
	public AbstractMeshOptions setLockPath(String lockPath) {
		return options.setLockPath(lockPath);
	}

	public String getAdminPassword() {
		return options.getAdminPassword();
	}

	@Setter
	public AbstractMeshOptions setAdminPassword(String adminPassword) {
		return options.setAdminPassword(adminPassword);
	}

	public String getInitialAdminPassword() {
		return options.getInitialAdminPassword();
	}

	@Setter
	public AbstractMeshOptions setInitialAdminPassword(String initialAdminPassword) {
		return options.setInitialAdminPassword(initialAdminPassword);
	}

	public boolean isForceInitialAdminPasswordReset() {
		return options.isForceInitialAdminPasswordReset();
	}

	@Setter
	public AbstractMeshOptions setForceInitialAdminPasswordReset(boolean forceInitialAdminPasswordReset) {
		return options.setForceInitialAdminPasswordReset(forceInitialAdminPasswordReset);
	}

	public int getPluginTimeout() {
		return options.getPluginTimeout();
	}

	@Setter
	public AbstractMeshOptions setPluginTimeout(int pluginTimeout) {
		return options.setPluginTimeout(pluginTimeout);
	}

	public boolean isStartInReadOnly() {
		return options.isStartInReadOnly();
	}

	@Setter
	public AbstractMeshOptions setStartInReadOnly(boolean startInReadOnly) {
		return options.setStartInReadOnly(startInReadOnly);
	}

	public int getVersionPurgeMaxBatchSize() {
		return options.getVersionPurgeMaxBatchSize();
	}

	@Setter
	public AbstractMeshOptions setVersionPurgeMaxBatchSize(int versionPurgeMaxBatchSize) {
		return options.setVersionPurgeMaxBatchSize(versionPurgeMaxBatchSize);
	}

	/**
	 * @see AbstractMeshOptions#validate()
	 */
	public void validate() {
		options.validate();
	}

	/**
	 * @see AbstractMeshOptions#validate(AbstractMeshOptions)
	 */
	public void validate(AbstractMeshOptions options) {
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
