package com.gentics.mesh.dagger.module;

import com.gentics.mesh.auth.MeshOAuthService;
import com.gentics.mesh.auth.oauth2.MeshOAuth2ServiceImpl;
import com.gentics.mesh.cache.CacheRegistry;
import com.gentics.mesh.cache.CacheRegistryImpl;
import com.gentics.mesh.cache.PermissionCache;
import com.gentics.mesh.cache.PermissionCacheImpl;
import com.gentics.mesh.cache.ProjectBranchNameCache;
import com.gentics.mesh.cache.ProjectBranchNameCacheImpl;
import com.gentics.mesh.cache.ProjectNameCache;
import com.gentics.mesh.cache.ProjectNameCacheImpl;
import com.gentics.mesh.cache.WebrootPathCache;
import com.gentics.mesh.cache.WebrootPathCacheImpl;
import com.gentics.mesh.cli.BootstrapInitializer;
import com.gentics.mesh.cli.BootstrapInitializerImpl;
import com.gentics.mesh.context.BulkActionContext;
import com.gentics.mesh.context.impl.BulkActionContextImpl;
import com.gentics.mesh.core.data.PersistenceClassMap;
import com.gentics.mesh.core.data.PersistenceClassMapImpl;
import com.gentics.mesh.core.data.binary.Binaries;
import com.gentics.mesh.core.data.binary.impl.BinariesImpl;
import com.gentics.mesh.core.data.dao.BinaryDaoWrapper;
import com.gentics.mesh.core.data.dao.BranchDaoWrapper;
import com.gentics.mesh.core.data.dao.GroupDaoWrapper;
import com.gentics.mesh.core.data.dao.JobDaoWrapper;
import com.gentics.mesh.core.data.dao.LanguageDaoWrapper;
import com.gentics.mesh.core.data.dao.MicroschemaDaoWrapper;
import com.gentics.mesh.core.data.dao.ProjectDaoWrapper;
import com.gentics.mesh.core.data.dao.RoleDaoWrapper;
import com.gentics.mesh.core.data.dao.SchemaDaoWrapper;
import com.gentics.mesh.core.data.dao.TagDaoWrapper;
import com.gentics.mesh.core.data.dao.TagFamilyDaoWrapper;
import com.gentics.mesh.core.data.dao.UserDaoWrapper;
import com.gentics.mesh.core.data.dao.impl.BinaryDaoWrapperImpl;
import com.gentics.mesh.core.data.dao.impl.BranchDaoWrapperImpl;
import com.gentics.mesh.core.data.dao.impl.GroupDaoWrapperImpl;
import com.gentics.mesh.core.data.dao.impl.JobDaoWrapperImpl;
import com.gentics.mesh.core.data.dao.impl.LanguageDaoWrapperImpl;
import com.gentics.mesh.core.data.dao.impl.MicroschemaDaoWrapperImpl;
import com.gentics.mesh.core.data.dao.impl.ProjectDaoWrapperImpl;
import com.gentics.mesh.core.data.dao.impl.RoleDaoWrapperImpl;
import com.gentics.mesh.core.data.dao.impl.SchemaDaoWrapperImpl;
import com.gentics.mesh.core.data.dao.impl.TagDaoWrapperImpl;
import com.gentics.mesh.core.data.dao.impl.TagFamilyDaoWrapperImpl;
import com.gentics.mesh.core.data.dao.impl.UserDaoWrapperImpl;
import com.gentics.mesh.core.data.service.WebRootService;
import com.gentics.mesh.core.data.service.WebRootServiceImpl;
import com.gentics.mesh.core.verticle.handler.WriteLock;
import com.gentics.mesh.core.verticle.handler.WriteLockImpl;
import com.gentics.mesh.distributed.RequestDelegator;
import com.gentics.mesh.distributed.coordinator.proxy.RequestDelegatorImpl;
import com.gentics.mesh.event.EventQueueBatch;
import com.gentics.mesh.event.impl.EventQueueBatchImpl;
import com.gentics.mesh.graphdb.OrientDBDatabase;
import com.gentics.mesh.graphdb.spi.Database;
import com.gentics.mesh.handler.RangeRequestHandler;
import com.gentics.mesh.handler.impl.RangeRequestHandlerImpl;
import com.gentics.mesh.metric.MetricsService;
import com.gentics.mesh.metric.MetricsServiceImpl;
import com.gentics.mesh.plugin.env.PluginEnvironment;
import com.gentics.mesh.plugin.manager.MeshPluginManager;
import com.gentics.mesh.plugin.manager.MeshPluginManagerImpl;
import com.gentics.mesh.plugin.pf4j.PluginEnvironmentImpl;
import com.gentics.mesh.plugin.registry.DelegatingPluginRegistry;
import com.gentics.mesh.plugin.registry.DelegatingPluginRegistryImpl;
import com.gentics.mesh.search.index.common.DropIndexHandler;
import com.gentics.mesh.search.index.common.DropIndexHandlerImpl;
import com.gentics.mesh.storage.BinaryStorage;
import com.gentics.mesh.storage.LocalBinaryStorage;

import dagger.Binds;
import dagger.Module;

@Module
public abstract class BindModule {

	@Binds
	abstract DropIndexHandler bindCommonHandler(DropIndexHandlerImpl e);

	@Binds
	abstract BootstrapInitializer bindBoot(BootstrapInitializerImpl e);

	@Binds
	abstract WebRootService bindWebrootService(WebRootServiceImpl e);

	@Binds
	abstract MeshOAuthService bindOAuthHandler(MeshOAuth2ServiceImpl e);

	@Binds
	abstract BinaryStorage bindBinaryStorage(LocalBinaryStorage e);

	@Binds
	abstract MetricsService bindMetricsService(MetricsServiceImpl e);

	@Binds
	abstract Database bindDatabase(OrientDBDatabase e);

	@Binds
	abstract RangeRequestHandler bindRangeRequestHandler(RangeRequestHandlerImpl e);

	@Binds
	abstract ProjectBranchNameCache bindBranchNameCache(ProjectBranchNameCacheImpl e);

	@Binds
	abstract WebrootPathCache bindWebrootPathCache(WebrootPathCacheImpl e);

	@Binds
	abstract PermissionCache bindPermissionCache(PermissionCacheImpl e);

	@Binds
	abstract ProjectNameCache bindProjectNameCache(ProjectNameCacheImpl e);

	@Binds
	abstract PluginEnvironment bindPluginEnv(PluginEnvironmentImpl e);

	@Binds
	abstract MeshPluginManager bindPluginManager(MeshPluginManagerImpl pm);

	@Binds
	abstract EventQueueBatch bindEventQueueBatch(EventQueueBatchImpl e);

	@Binds
	abstract BulkActionContext bindActionContext(BulkActionContextImpl e);

	@Binds
	abstract CacheRegistry bindCacheRegistry(CacheRegistryImpl e);

	@Binds
	abstract Binaries bindBinaries(BinariesImpl e);

	@Binds
	abstract PersistenceClassMap bindPersistenceClassMap(PersistenceClassMapImpl e);

	@Binds
	abstract RequestDelegator bindRequestDelegator(RequestDelegatorImpl e);

	@Binds
	abstract WriteLock bindWriteLock(WriteLockImpl e);

	@Binds
	abstract DelegatingPluginRegistry bindPluginRegistry(DelegatingPluginRegistryImpl e);

	// Daos

	@Binds
	abstract UserDaoWrapper bindUserDao(UserDaoWrapperImpl e);

	@Binds
	abstract RoleDaoWrapper bindRoleDao(RoleDaoWrapperImpl e);

	@Binds
	abstract GroupDaoWrapper bindGroupDao(GroupDaoWrapperImpl e);

	@Binds
	abstract ProjectDaoWrapper bindProjectDao(ProjectDaoWrapperImpl e);

	@Binds
	abstract JobDaoWrapper bindJobDao(JobDaoWrapperImpl e);

	@Binds
	abstract TagDaoWrapper bindTagDao(TagDaoWrapperImpl e);

	@Binds
	abstract TagFamilyDaoWrapper bindTagFamilyDao(TagFamilyDaoWrapperImpl e);

	@Binds
	abstract BinaryDaoWrapper bindBinaryDao(BinaryDaoWrapperImpl e);

	@Binds
	abstract BranchDaoWrapper bindBranchDao(BranchDaoWrapperImpl e);

	@Binds
	abstract SchemaDaoWrapper bindSchemaDao(SchemaDaoWrapperImpl e);

	@Binds
	abstract MicroschemaDaoWrapper bindMicroschemaDao(MicroschemaDaoWrapperImpl e);

	@Binds
	abstract LanguageDaoWrapper bindLanguageDao(LanguageDaoWrapperImpl e);

}
