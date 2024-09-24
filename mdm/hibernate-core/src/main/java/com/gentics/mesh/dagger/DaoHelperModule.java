package com.gentics.mesh.dagger;

import com.gentics.mesh.core.data.HibLanguage;
import com.gentics.mesh.core.data.branch.HibBranch;
import com.gentics.mesh.core.data.group.HibGroup;
import com.gentics.mesh.core.data.job.HibJob;
import com.gentics.mesh.core.data.node.HibNode;
import com.gentics.mesh.core.data.project.HibProject;
import com.gentics.mesh.core.data.role.HibRole;
import com.gentics.mesh.core.data.schema.HibMicroschema;
import com.gentics.mesh.core.data.schema.HibMicroschemaVersion;
import com.gentics.mesh.core.data.schema.HibSchema;
import com.gentics.mesh.core.data.schema.HibSchemaVersion;
import com.gentics.mesh.core.data.tag.HibTag;
import com.gentics.mesh.core.data.tagfamily.HibTagFamily;
import com.gentics.mesh.core.data.user.HibUser;
import com.gentics.mesh.hibernate.data.dao.DaoHelper;
import com.gentics.mesh.hibernate.data.dao.DaoHelperFactory;
import com.gentics.mesh.hibernate.data.dao.RootDaoHelper;
import com.gentics.mesh.hibernate.data.dao.RootDaoHelperFactory;
import com.gentics.mesh.hibernate.data.dao.helpers.CrossTableJoin;
import com.gentics.mesh.hibernate.data.dao.helpers.FieldJoin;
import com.gentics.mesh.hibernate.data.domain.HibBranchImpl;
import com.gentics.mesh.hibernate.data.domain.HibGroupImpl;
import com.gentics.mesh.hibernate.data.domain.HibJobImpl;
import com.gentics.mesh.hibernate.data.domain.HibLanguageImpl;
import com.gentics.mesh.hibernate.data.domain.HibMicroschemaImpl;
import com.gentics.mesh.hibernate.data.domain.HibMicroschemaVersionImpl;
import com.gentics.mesh.hibernate.data.domain.HibNodeImpl;
import com.gentics.mesh.hibernate.data.domain.HibProjectImpl;
import com.gentics.mesh.hibernate.data.domain.HibRoleImpl;
import com.gentics.mesh.hibernate.data.domain.HibSchemaImpl;
import com.gentics.mesh.hibernate.data.domain.HibSchemaVersionImpl;
import com.gentics.mesh.hibernate.data.domain.HibTagFamilyImpl;
import com.gentics.mesh.hibernate.data.domain.HibTagImpl;
import com.gentics.mesh.hibernate.data.domain.HibUserImpl;

import dagger.Module;
import dagger.Provides;

/**
 * A collection of entity DAO helpers.
 * 
 * @author plyhun
 *
 */
@Module
public class DaoHelperModule {

	@Provides
	DaoHelper<HibUser, HibUserImpl> user(DaoHelperFactory factory) {
		return factory.create(HibUserImpl.class);
	}

	@Provides
	DaoHelper<HibGroup, HibGroupImpl> group(DaoHelperFactory factory) {
		return factory.create(HibGroupImpl.class);
	}

	@Provides
	DaoHelper<HibRole, HibRoleImpl> role(DaoHelperFactory factory) {
		return factory.create(HibRoleImpl.class);
	}

	@Provides
	DaoHelper<HibSchema, HibSchemaImpl> schema(DaoHelperFactory factory) {
		return factory.create(HibSchemaImpl.class);
	}
	@Provides
	DaoHelper<HibSchemaVersion, HibSchemaVersionImpl> schemaVersion(DaoHelperFactory factory) {
		return factory.create(HibSchemaVersionImpl.class);
	}

	@Provides
	DaoHelper<HibMicroschema, HibMicroschemaImpl> microschema(DaoHelperFactory factory) {
		return factory.create(HibMicroschemaImpl.class);
	}

	@Provides
	DaoHelper<HibMicroschemaVersion, HibMicroschemaVersionImpl> microschemaVersion(DaoHelperFactory factory) {
		return factory.create(HibMicroschemaVersionImpl.class);
	}

	@Provides
	DaoHelper<HibProject, HibProjectImpl> project(DaoHelperFactory factory) {
		return factory.create(HibProjectImpl.class);
	}

	@Provides
	DaoHelper<HibBranch, HibBranchImpl> branch(DaoHelperFactory factory) {
		return factory.create(HibBranchImpl.class);
	}

	@Provides
	DaoHelper<HibNode, HibNodeImpl> node(DaoHelperFactory factory) {
		return factory.create(HibNodeImpl.class);
	}
	
	@Provides
	DaoHelper<HibLanguage, HibLanguageImpl> language(DaoHelperFactory factory) {
		return factory.create(HibLanguageImpl.class);
	}

	@Provides
	DaoHelper<HibJob, HibJobImpl> job(DaoHelperFactory factory) {
		return factory.create(HibJobImpl.class);
	}

	@Provides
	DaoHelper<HibTag, HibTagImpl> tag(DaoHelperFactory factory) {
		return factory.create(HibTagImpl.class);
	}

	@Provides
	DaoHelper<HibTagFamily, HibTagFamilyImpl> tagFamily(DaoHelperFactory factory) {
		return factory.create(HibTagFamilyImpl.class);
	}

	@Provides
	RootDaoHelper<HibNode, HibNodeImpl, HibProject, HibProjectImpl> nodeRoot(RootDaoHelperFactory factory, DaoHelperFactory daoHelperFactory) {
		DaoHelper<HibNode, HibNodeImpl> daoHelper = node(daoHelperFactory);
		return factory.create(daoHelper, new FieldJoin(daoHelper.getDomainClass(), HibProjectImpl.class, "project"));
	}

	@Provides
	RootDaoHelper<HibBranch, HibBranchImpl, HibProject, HibProjectImpl> branchRoot(RootDaoHelperFactory factory, DaoHelperFactory daoHelperFactory) {
		DaoHelper<HibBranch, HibBranchImpl> daoHelper = branch(daoHelperFactory);
		return factory.create(daoHelper, new FieldJoin(daoHelper.getDomainClass(), HibProjectImpl.class, "project"));
	}

	@Provides
	RootDaoHelper<HibTag, HibTagImpl, HibTagFamily, HibTagFamilyImpl> tagRoot(RootDaoHelperFactory factory, DaoHelperFactory daoHelperFactory) {
		DaoHelper<HibTag, HibTagImpl> daoHelper = tag(daoHelperFactory);
		return factory.create(daoHelper, new FieldJoin(daoHelper.getDomainClass(), HibTagFamilyImpl.class, "tagFamily"));
	}

	@Provides
	RootDaoHelper<HibTagFamily, HibTagFamilyImpl, HibProject, HibProjectImpl> tagFamilyRoot(RootDaoHelperFactory factory, DaoHelperFactory daoHelperFactory) {
		DaoHelper<HibTagFamily, HibTagFamilyImpl> daoHelper = tagFamily(daoHelperFactory);
		return factory.create(daoHelper, new FieldJoin(daoHelper.getDomainClass(), HibProjectImpl.class, "project"));
	}

	@Provides
	RootDaoHelper<HibSchema, HibSchemaImpl, HibProject, HibProjectImpl> schemaRoot(RootDaoHelperFactory factory, DaoHelperFactory daoHelperFactory) {
		DaoHelper<HibSchema, HibSchemaImpl> daoHelper = schema(daoHelperFactory);
		return factory.create(daoHelper, new CrossTableJoin(daoHelper.getDomainClass(), HibProjectImpl.class, "project_schema", "projects", "schemas"));
	}

	@Provides
	RootDaoHelper<HibMicroschema, HibMicroschemaImpl, HibProject, HibProjectImpl> microSchemaRoot(RootDaoHelperFactory factory, DaoHelperFactory daoHelperFactory) {
		DaoHelper<HibMicroschema, HibMicroschemaImpl> daoHelper = microschema(daoHelperFactory);
		return factory.create(daoHelper, new CrossTableJoin(daoHelper.getDomainClass(), HibProjectImpl.class, "project_microschema", "projects", "microschemas"));
	}
}
