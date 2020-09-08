package com.gentics.mesh.dagger.module;

import java.util.Arrays;
import java.util.List;

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

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
import com.gentics.mesh.changelog.ChangelogSystem;
import com.gentics.mesh.changelog.ChangelogSystemImpl;
import com.gentics.mesh.changelog.highlevel.HighLevelChangelogSystem;
import com.gentics.mesh.changelog.highlevel.HighLevelChangelogSystemImpl;
import com.gentics.mesh.cli.BootstrapInitializer;
import com.gentics.mesh.cli.BootstrapInitializerImpl;
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
import com.gentics.mesh.core.context.impl.GraphContextDataRegistryImpl;
import com.gentics.mesh.core.data.PersistenceClassMap;
import com.gentics.mesh.core.data.PersistenceClassMapImpl;
import com.gentics.mesh.core.data.binary.Binaries;
import com.gentics.mesh.core.data.binary.impl.BinariesImpl;
import com.gentics.mesh.core.data.dao.BinaryDaoWrapper;
import com.gentics.mesh.core.data.dao.BranchDaoWrapper;
import com.gentics.mesh.core.data.dao.ContentDaoWrapper;
import com.gentics.mesh.core.data.dao.DaoCollection;
import com.gentics.mesh.core.data.dao.GroupDaoWrapper;
import com.gentics.mesh.core.data.dao.JobDaoWrapper;
import com.gentics.mesh.core.data.dao.LanguageDaoWrapper;
import com.gentics.mesh.core.data.dao.MicroschemaDaoWrapper;
import com.gentics.mesh.core.data.dao.NodeDaoWrapper;
import com.gentics.mesh.core.data.dao.OrientDBDaoCollection;
import com.gentics.mesh.core.data.dao.PermissionRoots;
import com.gentics.mesh.core.data.dao.ProjectDaoWrapper;
import com.gentics.mesh.core.data.dao.RoleDaoWrapper;
import com.gentics.mesh.core.data.dao.SchemaDaoWrapper;
import com.gentics.mesh.core.data.dao.TagDaoWrapper;
import com.gentics.mesh.core.data.dao.TagFamilyDaoWrapper;
import com.gentics.mesh.core.data.dao.UserDaoWrapper;
import com.gentics.mesh.core.data.dao.impl.BinaryDaoWrapperImpl;
import com.gentics.mesh.core.data.dao.impl.BranchDaoWrapperImpl;
import com.gentics.mesh.core.data.dao.impl.ContentDaoWrapperImpl;
import com.gentics.mesh.core.data.dao.impl.GroupDaoWrapperImpl;
import com.gentics.mesh.core.data.dao.impl.JobDaoWrapperImpl;
import com.gentics.mesh.core.data.dao.impl.LanguageDaoWrapperImpl;
import com.gentics.mesh.core.data.dao.impl.MicroschemaDaoWrapperImpl;
import com.gentics.mesh.core.data.dao.impl.NodeDaoWrapperImpl;
import com.gentics.mesh.core.data.dao.impl.ProjectDaoWrapperImpl;
import com.gentics.mesh.core.data.dao.impl.RoleDaoWrapperImpl;
import com.gentics.mesh.core.data.dao.impl.SchemaDaoWrapperImpl;
import com.gentics.mesh.core.data.dao.impl.TagDaoWrapperImpl;
import com.gentics.mesh.core.data.dao.impl.TagFamilyDaoWrapperImpl;
import com.gentics.mesh.core.data.dao.impl.UserDaoWrapperImpl;
import com.gentics.mesh.core.data.generic.GraphUserPropertiesImpl;
import com.gentics.mesh.core.data.generic.PermissionProperties;
import com.gentics.mesh.core.data.generic.PermissionPropertiesImpl;
import com.gentics.mesh.core.data.generic.UserProperties;
import com.gentics.mesh.core.data.schema.handler.MicroschemaComparator;
import com.gentics.mesh.core.data.schema.handler.MicroschemaComparatorImpl;
import com.gentics.mesh.core.data.schema.handler.SchemaComparator;
import com.gentics.mesh.core.data.schema.handler.SchemaComparatorImpl;
import com.gentics.mesh.core.data.service.WebRootService;
import com.gentics.mesh.core.data.service.WebRootServiceImpl;
import com.gentics.mesh.core.endpoint.admin.consistency.ConsistencyCheck;
import com.gentics.mesh.core.endpoint.admin.consistency.check.BinaryCheck;
import com.gentics.mesh.core.endpoint.admin.consistency.check.BranchCheck;
import com.gentics.mesh.core.endpoint.admin.consistency.check.FieldCheck;
import com.gentics.mesh.core.endpoint.admin.consistency.check.GraphFieldContainerCheck;
import com.gentics.mesh.core.endpoint.admin.consistency.check.GroupCheck;
import com.gentics.mesh.core.endpoint.admin.consistency.check.MicronodeCheck;
import com.gentics.mesh.core.endpoint.admin.consistency.check.MicroschemaContainerCheck;
import com.gentics.mesh.core.endpoint.admin.consistency.check.NodeCheck;
import com.gentics.mesh.core.endpoint.admin.consistency.check.ProjectCheck;
import com.gentics.mesh.core.endpoint.admin.consistency.check.RoleCheck;
import com.gentics.mesh.core.endpoint.admin.consistency.check.SchemaContainerCheck;
import com.gentics.mesh.core.endpoint.admin.consistency.check.TagCheck;
import com.gentics.mesh.core.endpoint.admin.consistency.check.TagFamilyCheck;
import com.gentics.mesh.core.endpoint.admin.consistency.check.UserCheck;
import com.gentics.mesh.core.endpoint.node.BinaryUploadHandler;
import com.gentics.mesh.core.endpoint.node.BinaryUploadHandlerImpl;
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
import com.gentics.mesh.core.verticle.handler.WriteLock;
import com.gentics.mesh.core.verticle.handler.WriteLockImpl;
import com.gentics.mesh.distributed.RequestDelegator;
import com.gentics.mesh.distributed.coordinator.proxy.RequestDelegatorImpl;
import com.gentics.mesh.event.EventQueueBatch;
import com.gentics.mesh.event.impl.EventQueueBatchImpl;
import com.gentics.mesh.graphdb.OrientDBDatabase;
import com.gentics.mesh.graphdb.dagger.OrientDBCoreModule;
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
import com.gentics.mesh.rest.MeshLocalClient;
import com.gentics.mesh.rest.MeshLocalClientImpl;
import com.gentics.mesh.router.RouterStorageRegistry;
import com.gentics.mesh.router.RouterStorageRegistryImpl;
import com.gentics.mesh.search.index.common.DropIndexHandler;
import com.gentics.mesh.search.index.common.DropIndexHandlerImpl;
import com.gentics.mesh.search.index.group.GroupIndexHandler;
import com.gentics.mesh.search.index.group.GroupIndexHandlerImpl;
import com.gentics.mesh.search.index.microschema.MicroschemaContainerIndexHandlerImpl;
import com.gentics.mesh.search.index.microschema.MicroschemaIndexHandler;
import com.gentics.mesh.search.index.node.NodeIndexHandler;
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
import com.gentics.mesh.storage.BinaryStorage;
import com.gentics.mesh.storage.LocalBinaryStorage;
import com.gentics.mesh.storage.LocalBinaryStorageImpl;
import com.syncleus.ferma.ext.orientdb3.PermissionRootsImpl;

import dagger.Binds;
import dagger.Module;
import dagger.Provides;

@Module(includes = { OrientDBCoreModule.class })
public abstract class OrientDBModule {

	// Graph / ODB specific implementations

	@Binds
	abstract UserProperties userProperties(GraphUserPropertiesImpl e);

	@Binds
	abstract Database bindDatabase(OrientDBDatabase e);

	@Binds
	abstract UserDaoWrapper bindUserDao(UserDaoWrapperImpl e);

	@Binds
	abstract RoleDaoWrapper bindRoleDao(RoleDaoWrapperImpl e);

	@Binds
	abstract GroupDaoWrapper bindGroupDao(GroupDaoWrapperImpl e);

	@Binds
	abstract ProjectDaoWrapper bindProjectDao(ProjectDaoWrapperImpl e);

	@Binds
	abstract NodeDaoWrapper bindNodeDao(NodeDaoWrapperImpl e);

	@Binds
	abstract ContentDaoWrapper bindContentDao(ContentDaoWrapperImpl e);

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

	// END

	@Binds
	abstract DropIndexHandler bindCommonHandler(DropIndexHandlerImpl e);

	@Binds
	abstract BootstrapInitializer bindBoot(BootstrapInitializerImpl e);

	@Binds
	abstract WebRootService bindWebrootService(WebRootServiceImpl e);

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
	abstract Binaries bindBinaries(BinariesImpl e);

	@Binds
	abstract PersistenceClassMap bindPersistenceClassMap(PersistenceClassMapImpl e);

	@Binds
	abstract RequestDelegator bindRequestDelegator(RequestDelegatorImpl e);

	@Binds
	abstract WriteLock bindWriteLock(WriteLockImpl e);

	@Binds
	abstract DelegatingPluginRegistry bindPluginRegistry(DelegatingPluginRegistryImpl e);

	@Binds
	abstract ChangelogSystem bindChangelogSystem(ChangelogSystemImpl e);

	@Binds
	abstract HighLevelChangelogSystem bindHighLevelChangelogSystem(HighLevelChangelogSystemImpl e);

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
	abstract DaoCollection daoCollection(OrientDBDaoCollection daoCollection);

	@Binds
	abstract PermissionRoots permissionRoots(PermissionRootsImpl daoCollection);

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
	abstract MicroschemaComparator microschemaComparator(MicroschemaComparatorImpl e);

	@Binds
	abstract SchemaComparator schemaComparator(SchemaComparatorImpl e);

	@Binds
	abstract ContextDataRegistry contextDataRegistry(GraphContextDataRegistryImpl e);

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
	abstract PermissionProperties permissionProperties(PermissionPropertiesImpl e);

	@Binds
	abstract LocalBinaryStorage localBinaryStorage(LocalBinaryStorageImpl e);

	@Binds
	abstract WebRootLinkReplacer WebRootLinkReplacer(WebRootLinkReplacerImpl e);

	@Binds
	abstract BinaryUploadHandler binaryUploadHandler(BinaryUploadHandlerImpl e);

	@Binds
	abstract ProjectVersionPurgeHandler projectVersionPurgeHandler(ProjectVersionPurgeHandlerImpl e);

	@Provides
	public static List<ConsistencyCheck> consistencyCheckList() {
		return Arrays.asList(
			new GroupCheck(),
			new MicroschemaContainerCheck(),
			new NodeCheck(),
			new ProjectCheck(),
			new BranchCheck(),
			new RoleCheck(),
			new SchemaContainerCheck(),
			new TagCheck(),
			new TagFamilyCheck(),
			new UserCheck(),
			new GraphFieldContainerCheck(),
			new MicronodeCheck(),
			new BinaryCheck(),
			new FieldCheck());
	}

	// NON Graph Specific Bindings
	// TODO Move into dedicated module
	@Binds
	abstract MeshLocalClient meshLocalClient(MeshLocalClientImpl e);
}
