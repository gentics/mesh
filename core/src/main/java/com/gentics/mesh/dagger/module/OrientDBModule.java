package com.gentics.mesh.dagger.module;

import java.util.Arrays;
import java.util.List;

import com.gentics.mesh.changelog.ChangelogSystem;
import com.gentics.mesh.changelog.ChangelogSystemImpl;
import com.gentics.mesh.changelog.highlevel.HighLevelChangelogSystem;
import com.gentics.mesh.changelog.highlevel.HighLevelChangelogSystemImpl;
import com.gentics.mesh.core.data.dao.*;
import com.gentics.mesh.core.data.dao.impl.*;
import com.gentics.mesh.core.data.generic.GraphUserPropertiesImpl;
import com.gentics.mesh.core.data.generic.UserProperties;
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
import com.gentics.mesh.graphdb.OrientDBDatabase;
import com.gentics.mesh.graphdb.cluster.ClusterManager;
import com.gentics.mesh.graphdb.cluster.OrientDBClusterManager;
import com.gentics.mesh.graphdb.dagger.OrientDBCoreModule;
import com.gentics.mesh.graphdb.spi.Database;
import com.gentics.mesh.search.index.BucketManager;
import com.gentics.mesh.search.index.BucketManagerImpl;
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
	abstract UserProperties userProperties(GraphUserPropertiesImpl e);

	@Binds
	abstract Database bindDatabase(OrientDBDatabase e);

	@Binds
	abstract ChangelogSystem bindChangelogSystem(ChangelogSystemImpl e);

	@Binds
	abstract HighLevelChangelogSystem bindHighLevelChangelogSystem(HighLevelChangelogSystemImpl e);

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
	abstract S3BinaryDaoWrapper s3bindBinaryDao(S3BinaryDaoWrapperImpl e);

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
	abstract ClusterManager bindClusterManager(OrientDBClusterManager e);

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
