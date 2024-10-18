package com.gentics.mesh.dagger;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javax.inject.Singleton;

import com.gentics.mesh.cache.CacheRegistry;
import com.gentics.mesh.cache.GroupNameCache;
import com.gentics.mesh.cache.GroupNameCacheImpl;
import com.gentics.mesh.cache.HibCacheRegistry;
import com.gentics.mesh.cache.ListableFieldCache;
import com.gentics.mesh.cache.ListableFieldCacheImpl;
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
import com.gentics.mesh.changelog.HibernateBootstrapInitializerImpl;
import com.gentics.mesh.changelog.HibernateHighLevelChangesList;
import com.gentics.mesh.changelog.highlevel.HighLevelChangelogSystem;
import com.gentics.mesh.changelog.highlevel.HighLevelChangelogSystemImpl;
import com.gentics.mesh.changelog.highlevel.HighLevelChangesList;
import com.gentics.mesh.check.BinaryFieldRefCheck;
import com.gentics.mesh.check.BoolListItemCheck;
import com.gentics.mesh.check.ContentRefCheck;
import com.gentics.mesh.check.DateListItemCheck;
import com.gentics.mesh.check.HibernateBranchCheck;
import com.gentics.mesh.check.HtmlListItemCheck;
import com.gentics.mesh.check.MicronodeFieldRefCheck;
import com.gentics.mesh.check.MicronodeListItemCheck;
import com.gentics.mesh.check.NodeFieldContainerCheck;
import com.gentics.mesh.check.NodeFieldContainerVersionsEdgeCheck;
import com.gentics.mesh.check.NodeFieldRefCheck;
import com.gentics.mesh.check.NodeListItemCheck;
import com.gentics.mesh.check.NumberListItemCheck;
import com.gentics.mesh.check.S3BinaryFieldRefCheck;
import com.gentics.mesh.check.StringListItemCheck;
import com.gentics.mesh.cli.BootstrapInitializer;
import com.gentics.mesh.contentoperation.ContentCachedStorage;
import com.gentics.mesh.contentoperation.ContentStorage;
import com.gentics.mesh.contentoperation.ContentStorageImpl;
import com.gentics.mesh.core.data.binary.Binaries;
import com.gentics.mesh.core.data.dao.ChangelogDao;
import com.gentics.mesh.core.data.dao.DaoCollection;
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
import com.gentics.mesh.core.data.generic.UserProperties;
import com.gentics.mesh.core.data.root.RootResolver;
import com.gentics.mesh.core.data.s3binary.S3Binaries;
import com.gentics.mesh.core.data.service.WebRootService;
import com.gentics.mesh.core.db.Database;
import com.gentics.mesh.core.db.cluster.ClusterManager;
import com.gentics.mesh.core.endpoint.admin.AdminHandler;
import com.gentics.mesh.core.endpoint.admin.consistency.ConsistencyCheck;
import com.gentics.mesh.core.endpoint.admin.debuginfo.DebugInfoProvider;
import com.gentics.mesh.core.migration.BranchMigration;
import com.gentics.mesh.core.migration.MicronodeMigration;
import com.gentics.mesh.core.migration.NodeMigration;
import com.gentics.mesh.core.project.maintenance.ProjectVersionPurgeHandler;
import com.gentics.mesh.core.verticle.handler.WriteLock;
import com.gentics.mesh.dagger.tx.TransactionComponent;
import com.gentics.mesh.database.DatabaseProvider;
import com.gentics.mesh.database.DefaultSQLDatabase;
import com.gentics.mesh.database.HibernateDatabase;
import com.gentics.mesh.database.cluster.HibClusterManager;
import com.gentics.mesh.distributed.RequestDelegator;
import com.gentics.mesh.distributed.RequestDelegatorStub;
import com.gentics.mesh.endpoint.admin.HibAdminHandler;
import com.gentics.mesh.etc.config.HibernateMeshOptions;
import com.gentics.mesh.etc.config.MeshOptions;
import com.gentics.mesh.hibernate.HibernateRootResolver;
import com.gentics.mesh.hibernate.data.binary.impl.HibBinariesImpl;
import com.gentics.mesh.hibernate.data.dao.BinaryDaoImpl;
import com.gentics.mesh.hibernate.data.dao.BranchDaoImpl;
import com.gentics.mesh.hibernate.data.dao.ChangelogDaoImpl;
import com.gentics.mesh.hibernate.data.dao.ContentDaoImpl;
import com.gentics.mesh.hibernate.data.dao.GroupDaoImpl;
import com.gentics.mesh.hibernate.data.dao.HibDaoCollectionImpl;
import com.gentics.mesh.hibernate.data.dao.HibernateUserPropertiesImpl;
import com.gentics.mesh.hibernate.data.dao.ImageVariantDaoImpl;
import com.gentics.mesh.hibernate.data.dao.JobDaoImpl;
import com.gentics.mesh.hibernate.data.dao.LanguageDaoImpl;
import com.gentics.mesh.hibernate.data.dao.MicroschemaDaoImpl;
import com.gentics.mesh.hibernate.data.dao.NodeDaoImpl;
import com.gentics.mesh.hibernate.data.dao.ProjectDaoImpl;
import com.gentics.mesh.hibernate.data.dao.RoleDaoImpl;
import com.gentics.mesh.hibernate.data.dao.S3BinaryDaoImpl;
import com.gentics.mesh.hibernate.data.dao.SchemaDaoImpl;
import com.gentics.mesh.hibernate.data.dao.TagDaoImpl;
import com.gentics.mesh.hibernate.data.dao.TagFamilyDaoImpl;
import com.gentics.mesh.hibernate.data.dao.UserDaoImpl;
import com.gentics.mesh.hibernate.data.domain.AbstractHibListFieldEdgeImpl;
import com.gentics.mesh.hibernate.data.permission.HibPermissionRoots;
import com.gentics.mesh.hibernate.data.s3binary.impl.S3HibBinariesImpl;
import com.gentics.mesh.hibernate.data.service.HibWebRootServiceImpl;
import com.gentics.mesh.migration.HibBranchMigration;
import com.gentics.mesh.migration.HibMicronodeMigration;
import com.gentics.mesh.migration.HibNodeMigration;
import com.gentics.mesh.migration.HibProjectVersionPurge;
import com.gentics.mesh.search.index.BucketManager;
import com.gentics.mesh.search.index.BucketManagerImpl;
import com.gentics.mesh.verticle.handler.HibernateWriteLockImpl;
import com.hazelcast.core.HazelcastInstance;

import dagger.Binds;
import dagger.Module;
import dagger.Provides;
import dagger.multibindings.IntoSet;

/**
 * A collection of Dagger 2 binds and modules for Mesh Hibernate.
 * 
 * @author plyhun
 *
 */
@Module(includes = { DaoHelperModule.class, UtilModule.class, DatabaseConnectorModule.class }, subcomponents = TransactionComponent.class)
public abstract class HibernateModule {

	@Binds
	abstract HighLevelChangesList highLevelChangesList(HibernateHighLevelChangesList e);

	@Binds
	abstract Database bindDatabase(HibernateDatabase e);

	@Binds
	abstract RootResolver bindRootResolver(HibernateRootResolver rootResolver);
	
	@Binds
	abstract WriteLock bindWriteLock(HibernateWriteLockImpl e);

	@Binds
	abstract DatabaseProvider bindDatabaseProvider(DefaultSQLDatabase e);
	
	@Binds
	abstract BootstrapInitializer bindBootstrapInitializer(HibernateBootstrapInitializerImpl e);

	@Binds
	abstract ClusterManager bindClusterManager(HibClusterManager e);

	@Binds
	abstract HighLevelChangelogSystem bindHighLevelChangelogSystem(HighLevelChangelogSystemImpl e);
	
	@Binds
	abstract RequestDelegator bindRequestDelegator(RequestDelegatorStub e);

	@Binds
	abstract UserProperties userProperties(HibernateUserPropertiesImpl e);

	@Binds
	abstract WebRootService bindWebrootService(HibWebRootServiceImpl e);

	@Binds
	abstract AdminHandler adminHandler(HibAdminHandler e);

	@Binds
	abstract ContentStorage contentQuery(ContentStorageImpl e);

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
	abstract ListableFieldCache<AbstractHibListFieldEdgeImpl<?>> bindListableFieldCache(ListableFieldCacheImpl e);

	@Binds
	abstract CacheRegistry bindCacheRegistry(HibCacheRegistry e);

	// Migration
	@Binds
	abstract NodeMigration bindNodeMigration(HibNodeMigration e);

	@Binds
	abstract MicronodeMigration bindMicronodeMigration(HibMicronodeMigration e);

	@Binds
	abstract BranchMigration branchMigration(HibBranchMigration e);

	@Binds
	abstract ProjectVersionPurgeHandler projectVersionPurgeHandler(HibProjectVersionPurge e);

	// Daos

	@Binds
	abstract ChangelogDao changelogDao(ChangelogDaoImpl e);

	@Binds
	abstract DaoCollection daoCollection(HibDaoCollectionImpl daoCollection);

	@Binds
	abstract PermissionRoots permissionRoots(HibPermissionRoots daoCollection);

	@Binds
	abstract PersistingImageVariantDao bindImageVariantDao(ImageVariantDaoImpl dao);

	@Binds
	abstract PersistingUserDao bindUserDao(UserDaoImpl dao);

	@Binds
	abstract PersistingRoleDao bindRoleDao(RoleDaoImpl e);

	@Binds
	abstract PersistingGroupDao bindGroupDao(GroupDaoImpl e);

	@Binds
	abstract PersistingProjectDao bindProjectDao(ProjectDaoImpl e);

	@Binds
	abstract PersistingNodeDao bindNodeDao(NodeDaoImpl e);

	@Binds
	abstract PersistingContentDao bindContentDao(ContentDaoImpl e);

	@Binds
	abstract PersistingJobDao bindJobDao(JobDaoImpl e);

	@Binds
	abstract PersistingTagDao bindTagDao(TagDaoImpl e);

	@Binds
	abstract PersistingTagFamilyDao bindTagFamilyDao(TagFamilyDaoImpl e);

	@Binds
	abstract PersistingBinaryDao bindBinaryDao(BinaryDaoImpl e);

	@Binds
	abstract PersistingS3BinaryDao bindS3BinaryDao(S3BinaryDaoImpl e);

	@Binds
	abstract PersistingBranchDao bindBranchDao(BranchDaoImpl e);

	@Binds
	abstract PersistingSchemaDao bindSchemaDao(SchemaDaoImpl e);

	@Binds
	abstract PersistingMicroschemaDao bindMicroschemaDao(MicroschemaDaoImpl e);

	@Binds
	abstract PersistingLanguageDao bindLanguageDao(LanguageDaoImpl e);

	@Binds
	abstract BucketManager bindBucketManager(BucketManagerImpl e);

	@Binds
	abstract Binaries bindBinaries(HibBinariesImpl e);

	@Binds
	abstract S3Binaries bindS3Binaries(S3HibBinariesImpl e);

	@Provides
	public static HibernateMeshOptions hibernateMeshOptions(MeshOptions meshOptions) {
		if (meshOptions instanceof HibernateMeshOptions) {
			return (HibernateMeshOptions) meshOptions;
		} else {
			throw new IllegalArgumentException("Unsupported MeshOptions class:" + meshOptions.getClass().getCanonicalName());
		}
	}

	@Provides
	@Singleton
	public static HazelcastInstance hazelcast(HibClusterManager clusterManager) {
		return clusterManager.getHazelcast();
	}

	@Binds
	@IntoSet
	public abstract DebugInfoProvider contentCacheInfoProvider(ContentCachedStorage provider);

	@Binds
	@IntoSet
	public abstract DebugInfoProvider listableFieldCacheInfoProvider(ListableFieldCacheImpl provider);

	/**
	 * List of consistency checks. This is deliberately made a mutable lists, because some tests are currently written in a way that they create inconsistencies,
	 * therefore some consistency checks need to be excluded from the list when running tests
	 */
	protected static List<ConsistencyCheck> consistencyChecks = new ArrayList<>(
			Arrays.asList(new HibernateBranchCheck(),
					new BinaryFieldRefCheck(),
					new BoolListItemCheck(),
					new DateListItemCheck(),
					new HtmlListItemCheck(),
					new MicronodeFieldRefCheck(),
					new MicronodeListItemCheck(),
					new NodeFieldRefCheck(),
					new NodeListItemCheck(),
					new NumberListItemCheck(),
					new S3BinaryFieldRefCheck(),
					new StringListItemCheck(),
					new NodeFieldContainerCheck(),
					new NodeFieldContainerVersionsEdgeCheck(),
					new ContentRefCheck()));

	@Provides
	public static List<ConsistencyCheck> consistencyCheckList() {
		return consistencyChecks;
	}
}
