package com.gentics.mesh.core.db;

import com.gentics.mesh.core.action.BranchDAOActions;
import com.gentics.mesh.core.action.GroupDAOActions;
import com.gentics.mesh.core.action.MicroschemaDAOActions;
import com.gentics.mesh.core.action.ProjectDAOActions;
import com.gentics.mesh.core.action.RoleDAOActions;
import com.gentics.mesh.core.action.SchemaDAOActions;
import com.gentics.mesh.core.action.TagDAOActions;
import com.gentics.mesh.core.action.TagFamilyDAOActions;
import com.gentics.mesh.core.action.UserDAOActions;
import com.gentics.mesh.core.data.dao.BinaryDaoWrapper;
import com.gentics.mesh.core.data.dao.BranchDaoWrapper;
import com.gentics.mesh.core.data.dao.ContentDaoWrapper;
import com.gentics.mesh.core.data.dao.DaoCollection;
import com.gentics.mesh.core.data.dao.GroupDaoWrapper;
import com.gentics.mesh.core.data.dao.JobDaoWrapper;
import com.gentics.mesh.core.data.dao.LanguageDaoWrapper;
import com.gentics.mesh.core.data.dao.MicroschemaDaoWrapper;
import com.gentics.mesh.core.data.dao.NodeDaoWrapper;
import com.gentics.mesh.core.data.dao.ProjectDaoWrapper;
import com.gentics.mesh.core.data.dao.RoleDaoWrapper;
import com.gentics.mesh.core.data.dao.SchemaDaoWrapper;
import com.gentics.mesh.core.data.dao.TagDaoWrapper;
import com.gentics.mesh.core.data.dao.TagFamilyDaoWrapper;
import com.gentics.mesh.core.data.dao.UserDaoWrapper;
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

public class TxDataImpl implements TxData {

	private final DaoCollection daos;

	private final MeshOptions options;

	public TxDataImpl(MeshOptions options, DaoCollection daoCollection) {
		this.options = options;
		this.daos = daoCollection;
	}

	@Override
	public MeshOptions options() {
		return options;
	}

	@Override
	public UserDaoWrapper userDao() {
		return daos.userDao();
	}

	public UserDAOActions userActions() {
		return daos.userActions();
	}

	public GroupDAOActions groupActions() {
		return daos.groupActions();
	}

	public RoleDAOActions roleActions() {
		return daos.roleActions();
	}

	public ProjectDAOActions projectActions() {
		return daos.projectActions();
	}

	public TagFamilyDAOActions tagFamilyActions() {
		return daos.tagFamilyActions();
	}

	public TagDAOActions tagActions() {
		return daos.tagActions();
	}

	public BranchDAOActions branchActions() {
		return daos.branchActions();
	}

	public MicroschemaDAOActions microschemaActions() {
		return daos.microschemaActions();
	}

	public SchemaDAOActions schemaActions() {
		return daos.schemaActions();
	}

	public void overrideWithEnv() {
		options.overrideWithEnv();
	}

	public int hashCode() {
		return options.hashCode();
	}

	public boolean equals(Object obj) {
		return options.equals(obj);
	}

	public String getDefaultLanguage() {
		return options.getDefaultLanguage();
	}

	public MeshOptions setDefaultLanguage(String defaultLanguage) {
		return options.setDefaultLanguage(defaultLanguage);
	}

	public String getLanguagesFilePath() {
		return options.getLanguagesFilePath();
	}

	public MeshOptions setLanguagesFilePath(String languagesFilePath) {
		return options.setLanguagesFilePath(languagesFilePath);
	}

	public int getDefaultMaxDepth() {
		return options.getDefaultMaxDepth();
	}

	public MeshOptions setDefaultMaxDepth(int defaultMaxDepth) {
		return options.setDefaultMaxDepth(defaultMaxDepth);
	}

	public GraphStorageOptions getStorageOptions() {
		return options.getStorageOptions();
	}

	public MeshOptions setStorageOptions(GraphStorageOptions storageOptions) {
		return options.setStorageOptions(storageOptions);
	}

	public MeshUploadOptions getUploadOptions() {
		return options.getUploadOptions();
	}

	public MeshOptions setUploadOptions(MeshUploadOptions uploadOptions) {
		return options.setUploadOptions(uploadOptions);
	}

	public HttpServerConfig getHttpServerOptions() {
		return options.getHttpServerOptions();
	}

	public MeshOptions setHttpServerOptions(HttpServerConfig httpServerOptions) {
		return options.setHttpServerOptions(httpServerOptions);
	}

	public MonitoringConfig getMonitoringOptions() {
		return options.getMonitoringOptions();
	}

	public MeshOptions setMonitoringOptions(MonitoringConfig monitoringOptions) {
		return options.setMonitoringOptions(monitoringOptions);
	}

	public VertxOptions getVertxOptions() {
		return options.getVertxOptions();
	}

	public MeshOptions setVertxOptions(VertxOptions vertxOptions) {
		return options.setVertxOptions(vertxOptions);
	}

	public ClusterOptions getClusterOptions() {
		return options.getClusterOptions();
	}

	public MeshOptions setClusterOptions(ClusterOptions clusterOptions) {
		return options.setClusterOptions(clusterOptions);
	}

	public ElasticSearchOptions getSearchOptions() {
		return options.getSearchOptions();
	}

	public MeshOptions setSearchOptions(ElasticSearchOptions searchOptions) {
		return options.setSearchOptions(searchOptions);
	}

	public AuthenticationOptions getAuthenticationOptions() {
		return options.getAuthenticationOptions();
	}

	public MeshOptions setAuthenticationOptions(AuthenticationOptions authenticationOptions) {
		return options.setAuthenticationOptions(authenticationOptions);
	}

	public String getTempDirectory() {
		return options.getTempDirectory();
	}

	public MeshOptions setTempDirectory(String tempDirectory) {
		return options.setTempDirectory(tempDirectory);
	}

	public String toString() {
		return options.toString();
	}

	public String getPluginDirectory() {
		return options.getPluginDirectory();
	}

	public MeshOptions setPluginDirectory(String pluginDirectory) {
		return options.setPluginDirectory(pluginDirectory);
	}

	public ImageManipulatorOptions getImageOptions() {
		return options.getImageOptions();
	}

	public MeshOptions setImageOptions(ImageManipulatorOptions imageOptions) {
		return options.setImageOptions(imageOptions);
	}

	public ContentConfig getContentOptions() {
		return options.getContentOptions();
	}

	public MeshOptions setContentOptions(ContentConfig contentOptions) {
		return options.setContentOptions(contentOptions);
	}

	public DebugInfoOptions getDebugInfoOptions() {
		return options.getDebugInfoOptions();
	}

	public MeshOptions setDebugInfoOptions(DebugInfoOptions debugInfoOptions) {
		return options.setDebugInfoOptions(debugInfoOptions);
	}

	public CacheConfig getCacheConfig() {
		return options.getCacheConfig();
	}

	public MeshOptions setCacheConfig(CacheConfig cacheConfig) {
		return options.setCacheConfig(cacheConfig);
	}

	public boolean isUpdateCheckEnabled() {
		return options.isUpdateCheckEnabled();
	}

	public MeshOptions setUpdateCheck(boolean updateCheck) {
		return options.setUpdateCheck(updateCheck);
	}

	public String getNodeName() {
		return options.getNodeName();
	}

	public MeshOptions setNodeName(String nodeName) {
		return options.setNodeName(nodeName);
	}

	public boolean isInitClusterMode() {
		return options.isInitClusterMode();
	}

	public MeshOptions setInitCluster(boolean isInitCluster) {
		return options.setInitCluster(isInitCluster);
	}

	public String getLockPath() {
		return options.getLockPath();
	}

	public MeshOptions setLockPath(String lockPath) {
		return options.setLockPath(lockPath);
	}

	public String getAdminPassword() {
		return options.getAdminPassword();
	}

	public MeshOptions setAdminPassword(String adminPassword) {
		return options.setAdminPassword(adminPassword);
	}

	public String getInitialAdminPassword() {
		return options.getInitialAdminPassword();
	}

	public MeshOptions setInitialAdminPassword(String initialAdminPassword) {
		return options.setInitialAdminPassword(initialAdminPassword);
	}

	public boolean isForceInitialAdminPasswordReset() {
		return options.isForceInitialAdminPasswordReset();
	}

	public MeshOptions setForceInitialAdminPasswordReset(boolean forceInitialAdminPasswordReset) {
		return options.setForceInitialAdminPasswordReset(forceInitialAdminPasswordReset);
	}

	public int getPluginTimeout() {
		return options.getPluginTimeout();
	}

	public MeshOptions setPluginTimeout(int pluginTimeout) {
		return options.setPluginTimeout(pluginTimeout);
	}

	public boolean isStartInReadOnly() {
		return options.isStartInReadOnly();
	}

	public MeshOptions setStartInReadOnly(boolean startInReadOnly) {
		return options.setStartInReadOnly(startInReadOnly);
	}

	public int getVersionPurgeMaxBatchSize() {
		return options.getVersionPurgeMaxBatchSize();
	}

	public MeshOptions setVersionPurgeMaxBatchSize(int versionPurgeMaxBatchSize) {
		return options.setVersionPurgeMaxBatchSize(versionPurgeMaxBatchSize);
	}

	public void validate() {
		options.validate();
	}

	public void validate(MeshOptions options) {
		options.validate(options);
	}

	@Override
	public GroupDaoWrapper groupDao() {
		return daos.groupDao();
	}

	@Override
	public RoleDaoWrapper roleDao() {
		return daos.roleDao();
	}

	@Override
	public ProjectDaoWrapper projectDao() {
		return daos.projectDao();
	}

	@Override
	public JobDaoWrapper jobDao() {
		return daos.jobDao();
	}

	@Override
	public LanguageDaoWrapper languageDao() {
		return daos.languageDao();
	}

	@Override
	public SchemaDaoWrapper schemaDao() {
		return daos.schemaDao();
	}

	@Override
	public TagDaoWrapper tagDao() {
		return daos.tagDao();
	}

	@Override
	public TagFamilyDaoWrapper tagFamilyDao() {
		return daos.tagFamilyDao();
	}

	@Override
	public MicroschemaDaoWrapper microschemaDao() {
		return daos.microschemaDao();
	}

	@Override
	public BinaryDaoWrapper binaryDao() {
		return daos.binaryDao();
	}

	@Override
	public BranchDaoWrapper branchDao() {
		return daos.branchDao();
	}

	@Override
	public NodeDaoWrapper nodeDao() {
		return daos.nodeDao();
	}

	@Override
	public ContentDaoWrapper contentDao() {
		return daos.contentDao();
	}
}
