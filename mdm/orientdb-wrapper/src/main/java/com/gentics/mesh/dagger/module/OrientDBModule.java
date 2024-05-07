package com.gentics.mesh.dagger.module;

import java.util.Arrays;
import java.util.List;

import javax.inject.Singleton;

import com.gentics.mesh.cache.CacheRegistry;
import com.gentics.mesh.cache.GroupNameCache;
import com.gentics.mesh.cache.GroupNameCacheImpl;
import com.gentics.mesh.cache.OrientdbCacheRegistry;
import com.gentics.mesh.cache.ProjectBranchNameCache;
import com.gentics.mesh.cache.ProjectBranchNameCacheImpl;
import com.gentics.mesh.cache.ProjectNameCache;
import com.gentics.mesh.cache.ProjectNameCacheImpl;
import com.gentics.mesh.cache.RoleNameCache;
import com.gentics.mesh.cache.RoleNameCacheImpl;
import com.gentics.mesh.cache.TagFamilyNameCache;
import com.gentics.mesh.cache.TagFamilyNameCacheImpl;
import com.gentics.mesh.cache.TagNameCache;
import com.gentics.mesh.cache.TagNameCacheImpl;
import com.gentics.mesh.cache.UserNameCache;
import com.gentics.mesh.cache.UserNameCacheImpl;
import com.gentics.mesh.changelog.ChangelogSystem;
import com.gentics.mesh.changelog.ChangelogSystemImpl;
import com.gentics.mesh.changelog.highlevel.HighLevelChangelogSystem;
import com.gentics.mesh.changelog.highlevel.HighLevelChangelogSystemImpl;
import com.gentics.mesh.changelog.highlevel.HighLevelChangesList;
import com.gentics.mesh.changelog.highlevel.OrientDBHighLevelChangesList;
import com.gentics.mesh.cli.BootstrapInitializer;
import com.gentics.mesh.cli.OrientDBBootstrapInitializer;
import com.gentics.mesh.cli.OrientDBBootstrapInitializerImpl;
import com.gentics.mesh.core.data.PersistenceClassMap;
import com.gentics.mesh.core.data.PersistenceClassMapImpl;
import com.gentics.mesh.core.data.binary.Binaries;
import com.gentics.mesh.core.data.binary.impl.BinariesImpl;
import com.gentics.mesh.core.data.dao.BinaryDaoWrapper;
import com.gentics.mesh.core.data.dao.BranchDaoWrapper;
import com.gentics.mesh.core.data.dao.ContentDaoWrapper;
import com.gentics.mesh.core.data.dao.DaoCollection;
import com.gentics.mesh.core.data.dao.GroupDaoWrapper;
import com.gentics.mesh.core.data.dao.ImageVariantDaoWrapper;
import com.gentics.mesh.core.data.dao.JobDaoWrapper;
import com.gentics.mesh.core.data.dao.LanguageDaoWrapper;
import com.gentics.mesh.core.data.dao.MicroschemaDaoWrapper;
import com.gentics.mesh.core.data.dao.NodeDaoWrapper;
import com.gentics.mesh.core.data.dao.OrientDBDaoCollection;
import com.gentics.mesh.core.data.dao.PermissionRoots;
import com.gentics.mesh.core.data.dao.PersistingBinaryDao;
import com.gentics.mesh.core.data.dao.PersistingBranchDao;
import com.gentics.mesh.core.data.dao.PersistingContentDao;
import com.gentics.mesh.core.data.dao.PersistingGroupDao;
import com.gentics.mesh.core.data.dao.PersistingImageVariantDao;
import com.gentics.mesh.core.data.dao.PersistingJobDao;
import com.gentics.mesh.core.data.dao.PersistingLanguageDao;
import com.gentics.mesh.core.data.dao.PersistingMicroschemaDao;
import com.gentics.mesh.core.data.dao.PersistingNodeDao;
import com.gentics.mesh.core.data.dao.PersistingProjectDao;
import com.gentics.mesh.core.data.dao.PersistingRoleDao;
import com.gentics.mesh.core.data.dao.PersistingS3BinaryDao;
import com.gentics.mesh.core.data.dao.PersistingSchemaDao;
import com.gentics.mesh.core.data.dao.PersistingTagDao;
import com.gentics.mesh.core.data.dao.PersistingTagFamilyDao;
import com.gentics.mesh.core.data.dao.PersistingUserDao;
import com.gentics.mesh.core.data.dao.ProjectDaoWrapper;
import com.gentics.mesh.core.data.dao.RoleDaoWrapper;
import com.gentics.mesh.core.data.dao.S3BinaryDaoWrapper;
import com.gentics.mesh.core.data.dao.SchemaDaoWrapper;
import com.gentics.mesh.core.data.dao.TagDaoWrapper;
import com.gentics.mesh.core.data.dao.TagFamilyDaoWrapper;
import com.gentics.mesh.core.data.dao.UserDaoWrapper;
import com.gentics.mesh.core.data.dao.impl.BinaryDaoWrapperImpl;
import com.gentics.mesh.core.data.dao.impl.BranchDaoWrapperImpl;
import com.gentics.mesh.core.data.dao.impl.ContentDaoWrapperImpl;
import com.gentics.mesh.core.data.dao.impl.GroupDaoWrapperImpl;
import com.gentics.mesh.core.data.dao.impl.ImageVariantDaoWrapperImpl;
import com.gentics.mesh.core.data.dao.impl.JobDaoWrapperImpl;
import com.gentics.mesh.core.data.dao.impl.LanguageDaoWrapperImpl;
import com.gentics.mesh.core.data.dao.impl.MicroschemaDaoWrapperImpl;
import com.gentics.mesh.core.data.dao.impl.NodeDaoWrapperImpl;
import com.gentics.mesh.core.data.dao.impl.ProjectDaoWrapperImpl;
import com.gentics.mesh.core.data.dao.impl.RoleDaoWrapperImpl;
import com.gentics.mesh.core.data.dao.impl.S3BinaryDaoWrapperImpl;
import com.gentics.mesh.core.data.dao.impl.SchemaDaoWrapperImpl;
import com.gentics.mesh.core.data.dao.impl.TagDaoWrapperImpl;
import com.gentics.mesh.core.data.dao.impl.TagFamilyDaoWrapperImpl;
import com.gentics.mesh.core.data.dao.impl.UserDaoWrapperImpl;
import com.gentics.mesh.core.data.generic.GraphUserPropertiesImpl;
import com.gentics.mesh.core.data.generic.UserProperties;
import com.gentics.mesh.core.data.s3binary.S3Binaries;
import com.gentics.mesh.core.data.s3binary.impl.S3BinariesImpl;
import com.gentics.mesh.core.data.service.WebRootService;
import com.gentics.mesh.core.data.service.WebRootServiceImpl;
import com.gentics.mesh.core.db.CommonTxData;
import com.gentics.mesh.core.db.Database;
import com.gentics.mesh.core.db.cluster.ClusterManager;
import com.gentics.mesh.core.endpoint.admin.AdminHandler;
import com.gentics.mesh.core.endpoint.admin.OrientDBAdminHandler;
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
import com.gentics.mesh.core.migration.BranchMigration;
import com.gentics.mesh.core.migration.MicronodeMigration;
import com.gentics.mesh.core.migration.NodeMigration;
import com.gentics.mesh.core.migration.impl.BranchMigrationImpl;
import com.gentics.mesh.core.migration.impl.MicronodeMigrationImpl;
import com.gentics.mesh.core.migration.impl.NodeMigrationImpl;
import com.gentics.mesh.core.project.maintenance.ProjectVersionPurgeHandler;
import com.gentics.mesh.core.project.maintenance.ProjectVersionPurgeHandlerImpl;
import com.gentics.mesh.core.verticle.handler.OrientDBWriteLockImpl;
import com.gentics.mesh.core.verticle.handler.WriteLock;
import com.gentics.mesh.distributed.RequestDelegator;
import com.gentics.mesh.distributed.coordinator.proxy.ClusterEnabledRequestDelegatorImpl;
import com.gentics.mesh.etc.config.MeshOptions;
import com.gentics.mesh.etc.config.OrientDBMeshOptions;
import com.gentics.mesh.graphdb.OrientDBDatabase;
import com.gentics.mesh.graphdb.cluster.OrientDBClusterManager;
import com.gentics.mesh.graphdb.cluster.OrientDBClusterManagerImpl;
import com.gentics.mesh.graphdb.dagger.OrientDBCoreModule;
import com.gentics.mesh.graphdb.spi.GraphDatabase;
import com.gentics.mesh.graphdb.tx.impl.TxDataImpl;
import com.gentics.mesh.search.index.BucketManager;
import com.gentics.mesh.search.index.BucketManagerImpl;
import com.hazelcast.core.HazelcastInstance;
import com.syncleus.ferma.ext.orientdb3.PermissionRootsImpl;

import dagger.Binds;
import dagger.Module;
import dagger.Provides;

/**
 * Dagger module for OrientDB specific dependencies.
 */
@Module(includes = { OrientDBCoreModule.class })
public abstract class OrientDBModule {

	@Binds
	abstract HighLevelChangesList highLevelChangesList(OrientDBHighLevelChangesList e);

	@Binds
	abstract PersistenceClassMap bindPersistenceClassMap(PersistenceClassMapImpl e);

	@Binds
	abstract WebRootService bindWebrootService(WebRootServiceImpl e);

	@Binds
	abstract CommonTxData commonTxData(TxDataImpl e);

	@Binds
	abstract OrientDBClusterManager orientDBClusterManager(OrientDBClusterManagerImpl e);

	@Binds
	abstract UserProperties userProperties(GraphUserPropertiesImpl e);

	@Binds
	abstract Database bindDatabase(OrientDBDatabase e);

	@Binds
	abstract GraphDatabase bindGraphDatabase(OrientDBDatabase e);

	@Binds
	abstract BootstrapInitializer bindBootstrapInitializer(OrientDBBootstrapInitializerImpl e);

	@Binds
	abstract ChangelogSystem bindChangelogSystem(ChangelogSystemImpl e);

	@Binds
	abstract HighLevelChangelogSystem bindHighLevelChangelogSystem(HighLevelChangelogSystemImpl e);

	@Binds
	abstract WriteLock bindWriteLock(OrientDBWriteLockImpl e);

	@Binds
	abstract RequestDelegator bindRequestDelegator(ClusterEnabledRequestDelegatorImpl e);

	@Binds
	abstract Binaries bindBinaries(BinariesImpl e);

	@Binds
	abstract S3Binaries bindS3Binaries(S3BinariesImpl e);
	// DAOs

	@Binds
	abstract DaoCollection daoCollection(OrientDBDaoCollection daoCollection);

	@Binds
	abstract UserDaoWrapper bindUserDaoWrapper(UserDaoWrapperImpl e);

	@Binds
	abstract RoleDaoWrapper bindRoleDaoWrapper(RoleDaoWrapperImpl e);

	@Binds
	abstract GroupDaoWrapper bindGroupDaoWrapper(GroupDaoWrapperImpl e);

	@Binds
	abstract ProjectDaoWrapper bindProjectDaoWrapper(ProjectDaoWrapperImpl e);

	@Binds
	abstract NodeDaoWrapper bindNodeDaoWrapper(NodeDaoWrapperImpl e);

	@Binds
	abstract ContentDaoWrapper bindContentDaoWrapper(ContentDaoWrapperImpl e);

	@Binds
	abstract JobDaoWrapper bindJobDaoWrapper(JobDaoWrapperImpl e);

	@Binds
	abstract TagDaoWrapper bindTagDaoWrapper(TagDaoWrapperImpl e);

	@Binds
	abstract TagFamilyDaoWrapper bindTagFamilyDaoWrapper(TagFamilyDaoWrapperImpl e);

	@Binds
	abstract BinaryDaoWrapper bindBinaryDaoWrapper(BinaryDaoWrapperImpl e);

	@Binds
	abstract S3BinaryDaoWrapper bindS3BinaryDaoWrapper(S3BinaryDaoWrapperImpl e);

	@Binds
	abstract BranchDaoWrapper bindBranchDaoWrapper(BranchDaoWrapperImpl e);

	@Binds
	abstract SchemaDaoWrapper bindSchemaDaoWrapper(SchemaDaoWrapperImpl e);

	@Binds
	abstract MicroschemaDaoWrapper bindMicroschemaDaoWrapper(MicroschemaDaoWrapperImpl e);

	@Binds
	abstract LanguageDaoWrapper bindLanguageDaoWrapper(LanguageDaoWrapperImpl e);

	@Binds
	abstract ImageVariantDaoWrapper bindImageVariantDaoWrapper(ImageVariantDaoWrapperImpl e);

	@Binds
	abstract PersistingImageVariantDao bindImageVariantDao(ImageVariantDaoWrapper e);

	@Binds
	abstract PersistingUserDao bindUserDao(UserDaoWrapper e);

	@Binds
	abstract PersistingRoleDao bindRoleDao(RoleDaoWrapper e);

	@Binds
	abstract PersistingGroupDao bindGroupDao(GroupDaoWrapper e);

	@Binds
	abstract PersistingProjectDao bindProjectDao(ProjectDaoWrapper e);

	@Binds
	abstract PersistingNodeDao bindNodeDao(NodeDaoWrapper e);

	@Binds
	abstract PersistingContentDao bindContentDao(ContentDaoWrapper e);

	@Binds
	abstract PersistingJobDao bindJobDao(JobDaoWrapper e);

	@Binds
	abstract PersistingTagDao bindTagDao(TagDaoWrapper e);

	@Binds
	abstract PersistingTagFamilyDao bindTagFamilyDao(TagFamilyDaoWrapper e);

	@Binds
	abstract PersistingBinaryDao bindBinaryDao(BinaryDaoWrapper e);

	@Binds
	abstract PersistingS3BinaryDao s3BinaryDao(S3BinaryDaoWrapper e);

	@Binds
	abstract PersistingBranchDao bindBranchDao(BranchDaoWrapper e);

	@Binds
	abstract PersistingSchemaDao bindSchemaDao(SchemaDaoWrapper e);

	@Binds
	abstract PersistingMicroschemaDao bindMicroschemaDao(MicroschemaDaoWrapper e);

	@Binds
	abstract PersistingLanguageDao bindLanguageDao(LanguageDaoWrapper e);

	@Binds
	abstract AdminHandler adminHandler(OrientDBAdminHandler e);

	// Caches

	@Binds
	abstract UserNameCache bindUserNameCache(UserNameCacheImpl e);

	@Binds
	abstract TagFamilyNameCache bindTagFamilyNameCache(TagFamilyNameCacheImpl e);

	@Binds
	abstract TagNameCache bindTagNameCache(TagNameCacheImpl e);

	@Binds
	abstract RoleNameCache bindRoleNameCache(RoleNameCacheImpl e);

	@Binds
	abstract GroupNameCache bindGroupNameCache(GroupNameCacheImpl e);

	@Binds
	abstract ProjectBranchNameCache bindBranchNameCache(ProjectBranchNameCacheImpl e);

	@Binds
	abstract ProjectNameCache bindProjectNameCache(ProjectNameCacheImpl e);

	@Binds
	abstract CacheRegistry bindCacheRegistry(OrientdbCacheRegistry e);

	// Migration
	@Binds
	abstract NodeMigration nodeMigration(NodeMigrationImpl e);

	@Binds
	abstract MicronodeMigration micronodeMigration(MicronodeMigrationImpl e);

	@Binds
	abstract BranchMigration branchMigration(BranchMigrationImpl e);

	@Binds
	abstract ProjectVersionPurgeHandler projectVersionPurgeHandler(ProjectVersionPurgeHandlerImpl e);

	// END

	@Binds
	abstract BucketManager bindBucketManager(BucketManagerImpl e);

	@Binds
	abstract PermissionRoots permissionRoots(PermissionRootsImpl daoCollection);

	@Binds
	abstract ClusterManager bindClusterManager(OrientDBClusterManager e);

	@Binds
	abstract OrientDBBootstrapInitializer orientDBBootstrapInitializer(OrientDBBootstrapInitializerImpl e);

	@Provides
	public static OrientDBMeshOptions orientDBMeshOptions(MeshOptions meshOptions) {
		if (meshOptions instanceof OrientDBMeshOptions) {
			return (OrientDBMeshOptions) meshOptions;
		} else {
			throw new IllegalArgumentException("Unsupported MeshOptions class:" + meshOptions.getClass().getCanonicalName());
		}
	}

	/**
	 * Return the hazelcast instance which is fetched from OrientDB cluster manager.
	 *
	 * @param clusterManager
	 * @return
	 */
	@Provides
	@Singleton
	public static HazelcastInstance hazelcast(OrientDBClusterManager clusterManager) {
		return clusterManager.getHazelcast();
	}

	/**
	 * Return a list of all consistency checks.
	 * 
	 * @return
	 */
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
}
