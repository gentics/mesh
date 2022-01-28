package com.gentics.mesh.dagger.module;

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import com.gentics.mesh.auth.MeshOAuthService;
import com.gentics.mesh.auth.oauth2.MeshOAuth2ServiceImpl;
import com.gentics.mesh.cache.CacheCollection;
import com.gentics.mesh.cache.CacheCollectionImpl;
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
import com.gentics.mesh.context.BulkActionContext;
import com.gentics.mesh.context.impl.BulkActionContextImpl;
import com.gentics.mesh.core.action.BranchDAOActions;
import com.gentics.mesh.core.action.DAOActionsCollection;
import com.gentics.mesh.core.action.GroupDAOActions;
import com.gentics.mesh.core.action.JobDAOActions;
import com.gentics.mesh.core.action.MicroschemaDAOActions;
import com.gentics.mesh.core.action.NodeDAOActions;
import com.gentics.mesh.core.action.ProjectDAOActions;
import com.gentics.mesh.core.action.RoleDAOActions;
import com.gentics.mesh.core.action.SchemaDAOActions;
import com.gentics.mesh.core.action.TagDAOActions;
import com.gentics.mesh.core.action.TagFamilyDAOActions;
import com.gentics.mesh.core.action.UserDAOActions;
import com.gentics.mesh.core.actions.impl.BranchDAOActionsImpl;
import com.gentics.mesh.core.actions.impl.DAOActionsCollectionImpl;
import com.gentics.mesh.core.actions.impl.GroupDAOActionsImpl;
import com.gentics.mesh.core.actions.impl.JobDAOActionsImpl;
import com.gentics.mesh.core.actions.impl.MicroschemaDAOActionsImpl;
import com.gentics.mesh.core.actions.impl.NodeDAOActionsImpl;
import com.gentics.mesh.core.actions.impl.ProjectDAOActionsImpl;
import com.gentics.mesh.core.actions.impl.RoleDAOActionsImpl;
import com.gentics.mesh.core.actions.impl.SchemaDAOActionsImpl;
import com.gentics.mesh.core.actions.impl.TagDAOActionsImpl;
import com.gentics.mesh.core.actions.impl.TagFamilyDAOActionsImpl;
import com.gentics.mesh.core.actions.impl.UserDAOActionsImpl;
import com.gentics.mesh.core.binary.BinaryProcessorRegistry;
import com.gentics.mesh.core.binary.BinaryProcessorRegistryImpl;
import com.gentics.mesh.core.context.ContextDataRegistry;
import com.gentics.mesh.core.context.impl.ContextDataRegistryImpl;
import com.gentics.mesh.core.data.schema.handler.MicroschemaComparator;
import com.gentics.mesh.core.data.schema.handler.MicroschemaComparatorImpl;
import com.gentics.mesh.core.data.schema.handler.SchemaComparator;
import com.gentics.mesh.core.data.schema.handler.SchemaComparatorImpl;
import com.gentics.mesh.core.data.service.ServerSchemaStorage;
import com.gentics.mesh.core.data.service.ServerSchemaStorageImpl;
import com.gentics.mesh.core.db.CommonTxData;
import com.gentics.mesh.core.db.TxData;
import com.gentics.mesh.core.endpoint.node.BinaryUploadHandler;
import com.gentics.mesh.core.endpoint.node.BinaryUploadHandlerImpl;
import com.gentics.mesh.core.endpoint.node.S3BinaryUploadHandler;
import com.gentics.mesh.core.endpoint.node.S3BinaryUploadHandlerImpl;
import com.gentics.mesh.core.endpoint.role.RoleCrudHandler;
import com.gentics.mesh.core.endpoint.role.RoleCrudHandlerImpl;
import com.gentics.mesh.core.link.WebRootLinkReplacer;
import com.gentics.mesh.core.link.WebRootLinkReplacerImpl;
import com.gentics.mesh.core.migration.BranchMigration;
import com.gentics.mesh.core.migration.MicronodeMigration;
import com.gentics.mesh.core.migration.NodeMigration;
import com.gentics.mesh.core.migration.impl.BranchMigrationImpl;
import com.gentics.mesh.core.migration.impl.MicronodeMigrationImpl;
import com.gentics.mesh.core.migration.impl.NodeMigrationImpl;
import com.gentics.mesh.core.project.maintenance.ProjectVersionPurgeHandler;
import com.gentics.mesh.core.project.maintenance.ProjectVersionPurgeHandlerImpl;
import com.gentics.mesh.core.search.index.node.NodeIndexHandler;
import com.gentics.mesh.core.verticle.job.JobWorkerVerticle;
import com.gentics.mesh.core.verticle.job.JobWorkerVerticleImpl;
import com.gentics.mesh.distributed.TopologyChangeReadonlyHandler;
import com.gentics.mesh.distributed.TopologyChangeReadonlyHandlerImpl;
import com.gentics.mesh.event.EventQueueBatch;
import com.gentics.mesh.event.impl.EventQueueBatchImpl;
import com.gentics.mesh.handler.RangeRequestHandler;
import com.gentics.mesh.handler.impl.RangeRequestHandlerImpl;
import com.gentics.mesh.liveness.LivenessManagerImpl;
import com.gentics.mesh.metric.MetricsService;
import com.gentics.mesh.metric.MetricsServiceImpl;
import com.gentics.mesh.monitor.liveness.LivenessManager;
import com.gentics.mesh.plugin.env.PluginEnvironment;
import com.gentics.mesh.plugin.manager.MeshPluginManager;
import com.gentics.mesh.plugin.manager.MeshPluginManagerImpl;
import com.gentics.mesh.plugin.pf4j.PluginEnvironmentImpl;
import com.gentics.mesh.plugin.registry.DelegatingPluginRegistry;
import com.gentics.mesh.plugin.registry.DelegatingPluginRegistryImpl;
import com.gentics.mesh.rest.MeshLocalClient;
import com.gentics.mesh.rest.MeshLocalClientImpl;
import com.gentics.mesh.router.EndpointRegistry;
import com.gentics.mesh.router.EndpointRegistryImpl;
import com.gentics.mesh.router.RouterStorageRegistry;
import com.gentics.mesh.router.RouterStorageRegistryImpl;
import com.gentics.mesh.search.IndexHandlerRegistry;
import com.gentics.mesh.search.IndexHandlerRegistryImpl;
import com.gentics.mesh.search.SearchMappingsCache;
import com.gentics.mesh.search.impl.SearchMappingsCacheImpl;
import com.gentics.mesh.search.index.common.DropIndexHandler;
import com.gentics.mesh.search.index.common.DropIndexHandlerImpl;
import com.gentics.mesh.search.index.group.GroupIndexHandler;
import com.gentics.mesh.search.index.group.GroupIndexHandlerImpl;
import com.gentics.mesh.search.index.microschema.MicroschemaContainerIndexHandlerImpl;
import com.gentics.mesh.search.index.microschema.MicroschemaIndexHandler;
import com.gentics.mesh.search.index.node.NodeIndexHandlerImpl;
import com.gentics.mesh.search.index.project.ProjectIndexHandler;
import com.gentics.mesh.search.index.project.ProjectIndexHandlerImpl;
import com.gentics.mesh.search.index.role.RoleIndexHandler;
import com.gentics.mesh.search.index.role.RoleIndexHandlerImpl;
import com.gentics.mesh.search.index.schema.SchemaContainerIndexHandlerImpl;
import com.gentics.mesh.search.index.schema.SchemaIndexHandler;
import com.gentics.mesh.search.index.tag.TagIndexHandler;
import com.gentics.mesh.search.index.tag.TagIndexHandlerImpl;
import com.gentics.mesh.search.index.tagfamily.TagFamilyIndexHandler;
import com.gentics.mesh.search.index.tagfamily.TagFamilyIndexHandlerImpl;
import com.gentics.mesh.search.index.user.UserIndexHandler;
import com.gentics.mesh.search.index.user.UserIndexHandlerImpl;
import com.gentics.mesh.security.SecurityUtils;
import com.gentics.mesh.security.SecurityUtilsImpl;
import com.gentics.mesh.core.data.storage.BinaryStorage;
import com.gentics.mesh.core.data.storage.LocalBinaryStorage;
import com.gentics.mesh.storage.LocalBinaryStorageImpl;
import com.gentics.mesh.core.data.storage.S3BinaryStorage;
import com.gentics.mesh.core.data.storage.s3.S3BinaryStorageImpl;

import dagger.Binds;
import dagger.Module;

/**
 * Dagger module for common bindings
 */
@Module
public abstract class CommonBindModule {
	@Binds
	abstract TxData txData(CommonTxData e);

	@Binds
	abstract ContextDataRegistry contextDataRegistry(ContextDataRegistryImpl e);

	@Binds
	abstract DropIndexHandler bindCommonHandler(DropIndexHandlerImpl e);

	@Binds
	abstract MeshOAuthService bindOAuthHandler(MeshOAuth2ServiceImpl e);

	@Binds
	abstract BinaryStorage bindBinaryStorage(LocalBinaryStorageImpl e);

	@Binds
	abstract MetricsService bindMetricsService(MetricsServiceImpl e);

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
	abstract S3BinaryStorage bindS3BinaryStorage(S3BinaryStorageImpl e);

	@Binds
	abstract DelegatingPluginRegistry bindPluginRegistry(DelegatingPluginRegistryImpl e);

	@Binds
	abstract RouterStorageRegistry bindRouterStorageRegistry(RouterStorageRegistryImpl e);

	@Binds
	abstract PasswordEncoder bindPasswordEncoder(BCryptPasswordEncoder e);

	@Binds
	abstract NodeIndexHandler bindNodeIndexHandler(NodeIndexHandlerImpl e);

	@Binds
	abstract BinaryProcessorRegistry bindBinaryProcessorRegistry(BinaryProcessorRegistryImpl e);

	@Binds
	abstract DAOActionsCollection bindDaoCollection(DAOActionsCollectionImpl e);

	@Binds
	abstract MicroschemaComparator microschemaComparator(MicroschemaComparatorImpl e);

	@Binds
	abstract SchemaComparator schemaComparator(SchemaComparatorImpl e);

	@Binds
	abstract RoleIndexHandler roleIndexHandler(RoleIndexHandlerImpl e);

	@Binds
	abstract UserIndexHandler userIndexHandler(UserIndexHandlerImpl e);

	@Binds
	abstract ProjectIndexHandler projectIndexHandler(ProjectIndexHandlerImpl e);

	@Binds
	abstract GroupIndexHandler groupIndexHandler(GroupIndexHandlerImpl e);

	@Binds
	abstract SchemaIndexHandler schemaIndexHandler(SchemaContainerIndexHandlerImpl e);

	@Binds
	abstract MicroschemaIndexHandler microschemaIndexHandler(MicroschemaContainerIndexHandlerImpl e);

	@Binds
	abstract TagIndexHandler tagIndexHandler(TagIndexHandlerImpl e);

	@Binds
	abstract TagFamilyIndexHandler tagFamilyIndexHandler(TagFamilyIndexHandlerImpl e);

	@Binds
	abstract BranchMigration branchMigration(BranchMigrationImpl e);

	@Binds
	abstract MicronodeMigration micronodeMigration(MicronodeMigrationImpl e);

	@Binds
	abstract NodeMigration nodeMigration(NodeMigrationImpl e);

	@Binds
	abstract LocalBinaryStorage localBinaryStorage(LocalBinaryStorageImpl e);

	@Binds
	abstract WebRootLinkReplacer WebRootLinkReplacer(WebRootLinkReplacerImpl e);

	@Binds
	abstract BinaryUploadHandler binaryUploadHandler(BinaryUploadHandlerImpl e);

	@Binds
	abstract S3BinaryUploadHandler s3binaryUploadHandler(S3BinaryUploadHandlerImpl e);

	@Binds
	abstract ProjectVersionPurgeHandler projectVersionPurgeHandler(ProjectVersionPurgeHandlerImpl e);

	@Binds
	abstract ServerSchemaStorage serverSchemaStorage(ServerSchemaStorageImpl e);

	@Binds
	abstract RoleCrudHandler roleCrudHandler(RoleCrudHandlerImpl e);

	@Binds
	abstract EndpointRegistry endpointRegistry(EndpointRegistryImpl e);

	@Binds
	abstract IndexHandlerRegistry indexHandlerRegistry(IndexHandlerRegistryImpl e);

	@Binds
	abstract JobWorkerVerticle jobWorkerVerticle(JobWorkerVerticleImpl e);

	@Binds
	abstract UserDAOActions userDAOActions(UserDAOActionsImpl e);

	@Binds
	abstract GroupDAOActions grouDAOpActions(GroupDAOActionsImpl e);

	@Binds
	abstract RoleDAOActions roleDAOActions(RoleDAOActionsImpl e);

	@Binds
	abstract TagDAOActions tagADAOctions(TagDAOActionsImpl e);

	@Binds
	abstract TagFamilyDAOActions tagFDAOamilyActions(TagFamilyDAOActionsImpl e);

	@Binds
	abstract BranchDAOActions branDAOchActions(BranchDAOActionsImpl e);

	@Binds
	abstract ProjectDAOActions projDAOectActions(ProjectDAOActionsImpl e);

	@Binds
	abstract NodeDAOActions nodeDAOActions(NodeDAOActionsImpl e);

	@Binds
	abstract MicroschemaDAOActions microschemaDAOActions(MicroschemaDAOActionsImpl e);

	@Binds
	abstract SchemaDAOActions schemaDAOActions(SchemaDAOActionsImpl e);

	@Binds
	abstract JobDAOActions jobDAOActions(JobDAOActionsImpl e);

	@Binds
	abstract MeshLocalClient meshLocalClient(MeshLocalClientImpl e);

	@Binds
	abstract CacheCollection bindCacheCollection(CacheCollectionImpl e);

	@Binds
	abstract SecurityUtils bindSecurityUtils(SecurityUtilsImpl e);

	@Binds
	abstract TopologyChangeReadonlyHandler bindTopologyChangeReadonlyHandler(TopologyChangeReadonlyHandlerImpl e);

	@Binds
	abstract LivenessManager bindLivenessManager(LivenessManagerImpl e);

	@Binds
	abstract SearchMappingsCache searchMappingsCache(SearchMappingsCacheImpl e);
}
