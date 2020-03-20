package com.gentics.mesh.dagger.module;

import com.gentics.mesh.auth.MeshOAuth2ServiceImpl;
import com.gentics.mesh.auth.MeshOAuthService;
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
import com.gentics.mesh.core.data.generic.PermissionProperties;
import com.gentics.mesh.core.data.generic.PermissionPropertiesImpl;
import com.gentics.mesh.core.data.generic.UserProperties;
import com.gentics.mesh.core.data.generic.UserPropertiesImpl;
import com.gentics.mesh.core.data.service.ServerSchemaStorageImpl;
import com.gentics.mesh.core.data.service.WebRootService;
import com.gentics.mesh.core.data.service.WebRootServiceImpl;
import com.gentics.mesh.core.endpoint.migration.branch.BranchMigrationHandlerImpl;
import com.gentics.mesh.core.endpoint.migration.micronode.MicronodeMigrationHandlerImpl;
import com.gentics.mesh.core.endpoint.migration.node.NodeMigrationHandlerImpl;
import com.gentics.mesh.core.migration.branch.BranchMigrationHandler;
import com.gentics.mesh.core.migration.micronode.MicronodeMigrationHandler;
import com.gentics.mesh.core.migration.node.NodeMigrationHandler;
import com.gentics.mesh.core.project.maintenance.ProjectVersionPurgeHandler;
import com.gentics.mesh.core.project.maintenance.ProjectVersionPurgeHandlerImpl;
import com.gentics.mesh.core.rest.schema.ServerSchemaStorage;
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
import com.gentics.mesh.search.index.common.DropIndexHandler;
import com.gentics.mesh.search.index.common.DropIndexHandlerImpl;
import com.gentics.mesh.search.index.node.NodeIndexHandler;
import com.gentics.mesh.search.index.node.NodeIndexHandlerImpl;
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
	abstract UserProperties bindUserProperties(UserPropertiesImpl e);

	@Binds
	abstract PermissionProperties bindPermissionProperties(PermissionPropertiesImpl e);

	@Binds
	abstract ProjectVersionPurgeHandler bindProjectVersionPurgeHandler(ProjectVersionPurgeHandlerImpl e);

	@Binds
	abstract NodeMigrationHandler bindNodeMigrationHandler(NodeMigrationHandlerImpl e);

	@Binds
	abstract BranchMigrationHandler bindBranchMigrationHandler(BranchMigrationHandlerImpl e);

	@Binds 
	abstract MicronodeMigrationHandler bindMicronodeMigrationHandler(MicronodeMigrationHandlerImpl e);

	@Binds
	abstract ServerSchemaStorage bindServerSchemaStorage(ServerSchemaStorageImpl e);

	@Binds
	abstract NodeIndexHandler bindNodeIndexHandler(NodeIndexHandlerImpl e);

}
