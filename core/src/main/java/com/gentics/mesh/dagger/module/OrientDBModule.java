package com.gentics.mesh.dagger.module;

import java.util.Arrays;
import java.util.List;

import javax.inject.Singleton;

import com.gentics.mesh.changelog.ChangelogSystem;
import com.gentics.mesh.changelog.ChangelogSystemImpl;
import com.gentics.mesh.changelog.highlevel.HighLevelChangelogSystem;
import com.gentics.mesh.changelog.highlevel.HighLevelChangelogSystemImpl;
import com.gentics.mesh.cli.BootstrapInitializer;
import com.gentics.mesh.cli.OrientDBBootstrapInitializer;
import com.gentics.mesh.cli.OrientDBBootstrapInitializerImpl;
import com.gentics.mesh.core.data.dao.*;
import com.gentics.mesh.core.data.dao.impl.*;
import com.gentics.mesh.core.data.generic.GraphUserPropertiesImpl;
import com.gentics.mesh.core.data.generic.UserProperties;
import com.gentics.mesh.core.db.Database;
import com.gentics.mesh.core.db.cluster.ClusterManager;
import com.gentics.mesh.core.endpoint.admin.AdminHandler;
import com.gentics.mesh.core.endpoint.admin.ClusterAdminHandler;
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
import com.gentics.mesh.core.verticle.handler.WriteLock;
import com.gentics.mesh.core.verticle.handler.WriteLockImpl;
import com.gentics.mesh.distributed.RequestDelegator;
import com.gentics.mesh.distributed.coordinator.proxy.ClusterEnabledRequestDelegatorImpl;
import com.gentics.mesh.etc.config.MeshOptions;
import com.gentics.mesh.etc.config.OrientDBMeshOptions;
import com.gentics.mesh.graphdb.OrientDBDatabase;
import com.gentics.mesh.graphdb.cluster.OrientDBClusterManager;
import com.gentics.mesh.graphdb.cluster.OrientDBClusterManagerImpl;
import com.gentics.mesh.graphdb.dagger.OrientDBCoreModule;
import com.gentics.mesh.graphdb.spi.GraphDatabase;
import com.gentics.mesh.rest.MeshLocalClusterClient;
import com.gentics.mesh.rest.MeshLocalClusterClientImpl;
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
	abstract WriteLock bindWriteLock(WriteLockImpl e);

	@Binds
	abstract ClusterAdminHandler bindClusterAdminHandler(OrientDBAdminHandler e);

	@Binds
	abstract AdminHandler bindAdminHandler(ClusterAdminHandler e);

	@Binds
	abstract MeshLocalClusterClient meshLocalClusterClient(MeshLocalClusterClientImpl e);

	@Binds
	abstract RequestDelegator bindRequestDelegator(ClusterEnabledRequestDelegatorImpl e);

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
	abstract UserDao bindUserDao(UserDaoWrapper e);

	@Binds
	abstract RoleDao bindRoleDao(RoleDaoWrapper e);

	@Binds
	abstract GroupDao bindGroupDao(GroupDaoWrapper e);

	@Binds
	abstract ProjectDao bindProjectDao(ProjectDaoWrapper e);

	@Binds
	abstract NodeDao bindNodeDao(NodeDaoWrapper e);

	@Binds
	abstract ContentDao bindContentDao(ContentDaoWrapper e);

	@Binds
	abstract JobDao bindJobDao(JobDaoWrapper e);

	@Binds
	abstract TagDao bindTagDao(TagDaoWrapper e);

	@Binds
	abstract TagFamilyDao bindTagFamilyDao(TagFamilyDaoWrapper e);

	@Binds
	abstract BinaryDao bindBinaryDao(BinaryDaoWrapper e);

	@Binds
	abstract S3BinaryDao s3BinaryDao(S3BinaryDaoWrapper e);

	@Binds
	abstract BranchDao bindBranchDao(BranchDaoWrapper e);

	@Binds
	abstract SchemaDao bindSchemaDao(SchemaDaoWrapper e);

	@Binds
	abstract MicroschemaDao bindMicroschemaDao(MicroschemaDaoWrapper e);

	@Binds
	abstract LanguageDao bindLanguageDao(LanguageDaoWrapper e);

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
