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
import com.gentics.mesh.core.data.generic.UserProperties;
import com.gentics.mesh.core.db.Database;
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
import com.gentics.mesh.etc.config.MeshOptions;
import com.gentics.mesh.etc.config.OrientDBMeshOptions;
import com.gentics.mesh.graphdb.OrientDBDatabase;
import com.gentics.mesh.graphdb.cluster.OrientDBClusterManager;
import com.gentics.mesh.graphdb.cluster.OrientDBClusterManagerImpl;
import com.gentics.mesh.graphdb.dagger.OrientDBCoreModule;
import com.gentics.mesh.graphdb.spi.GraphDatabase;
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

	// DAOs

	@Binds
	abstract DaoCollection daoCollection(OrientDBDaoCollection daoCollection);

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
	abstract BucketManager bindBucketManager(BucketManagerImpl e);

	@Binds
	abstract PermissionRoots permissionRoots(PermissionRootsImpl daoCollection);

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
	 * @param db
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
