package com.gentics.mesh.dagger;

import com.gentics.mesh.core.data.Language;
import com.gentics.mesh.core.data.branch.Branch;
import com.gentics.mesh.core.data.group.Group;
import com.gentics.mesh.core.data.job.Job;
import com.gentics.mesh.core.data.node.Node;
import com.gentics.mesh.core.data.project.Project;
import com.gentics.mesh.core.data.role.Role;
import com.gentics.mesh.core.data.schema.Microschema;
import com.gentics.mesh.core.data.schema.MicroschemaVersion;
import com.gentics.mesh.core.data.schema.Schema;
import com.gentics.mesh.core.data.schema.SchemaVersion;
import com.gentics.mesh.core.data.tag.Tag;
import com.gentics.mesh.core.data.tagfamily.TagFamily;
import com.gentics.mesh.core.data.user.User;
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
	DaoHelper<User, HibUserImpl> user(DaoHelperFactory factory) {
		return factory.create(HibUserImpl.class);
	}

	@Provides
	DaoHelper<Group, HibGroupImpl> group(DaoHelperFactory factory) {
		return factory.create(HibGroupImpl.class);
	}

	@Provides
	DaoHelper<Role, HibRoleImpl> role(DaoHelperFactory factory) {
		return factory.create(HibRoleImpl.class);
	}

	@Provides
	DaoHelper<Schema, HibSchemaImpl> schema(DaoHelperFactory factory) {
		return factory.create(HibSchemaImpl.class);
	}
	@Provides
	DaoHelper<SchemaVersion, HibSchemaVersionImpl> schemaVersion(DaoHelperFactory factory) {
		return factory.create(HibSchemaVersionImpl.class);
	}

	@Provides
	DaoHelper<Microschema, HibMicroschemaImpl> microschema(DaoHelperFactory factory) {
		return factory.create(HibMicroschemaImpl.class);
	}

	@Provides
	DaoHelper<MicroschemaVersion, HibMicroschemaVersionImpl> microschemaVersion(DaoHelperFactory factory) {
		return factory.create(HibMicroschemaVersionImpl.class);
	}

	@Provides
	DaoHelper<Project, HibProjectImpl> project(DaoHelperFactory factory) {
		return factory.create(HibProjectImpl.class);
	}

	@Provides
	DaoHelper<Branch, HibBranchImpl> branch(DaoHelperFactory factory) {
		return factory.create(HibBranchImpl.class);
	}

	@Provides
	DaoHelper<Node, HibNodeImpl> node(DaoHelperFactory factory) {
		return factory.create(HibNodeImpl.class);
	}
	
	@Provides
	DaoHelper<Language, HibLanguageImpl> language(DaoHelperFactory factory) {
		return factory.create(HibLanguageImpl.class);
	}

	@Provides
	DaoHelper<Job, HibJobImpl> job(DaoHelperFactory factory) {
		return factory.create(HibJobImpl.class);
	}

	@Provides
	DaoHelper<Tag, HibTagImpl> tag(DaoHelperFactory factory) {
		return factory.create(HibTagImpl.class);
	}

	@Provides
	DaoHelper<TagFamily, HibTagFamilyImpl> tagFamily(DaoHelperFactory factory) {
		return factory.create(HibTagFamilyImpl.class);
	}

	@Provides
	RootDaoHelper<Node, HibNodeImpl, Project, HibProjectImpl> nodeRoot(RootDaoHelperFactory factory, DaoHelperFactory daoHelperFactory) {
		DaoHelper<Node, HibNodeImpl> daoHelper = node(daoHelperFactory);
		return factory.create(daoHelper, new FieldJoin(daoHelper.getDomainClass(), HibProjectImpl.class, "project"));
	}

	@Provides
	RootDaoHelper<Branch, HibBranchImpl, Project, HibProjectImpl> branchRoot(RootDaoHelperFactory factory, DaoHelperFactory daoHelperFactory) {
		DaoHelper<Branch, HibBranchImpl> daoHelper = branch(daoHelperFactory);
		return factory.create(daoHelper, new FieldJoin(daoHelper.getDomainClass(), HibProjectImpl.class, "project"));
	}

	@Provides
	RootDaoHelper<Tag, HibTagImpl, TagFamily, HibTagFamilyImpl> tagRoot(RootDaoHelperFactory factory, DaoHelperFactory daoHelperFactory) {
		DaoHelper<Tag, HibTagImpl> daoHelper = tag(daoHelperFactory);
		return factory.create(daoHelper, new FieldJoin(daoHelper.getDomainClass(), HibTagFamilyImpl.class, "tagFamily"));
	}

	@Provides
	RootDaoHelper<TagFamily, HibTagFamilyImpl, Project, HibProjectImpl> tagFamilyRoot(RootDaoHelperFactory factory, DaoHelperFactory daoHelperFactory) {
		DaoHelper<TagFamily, HibTagFamilyImpl> daoHelper = tagFamily(daoHelperFactory);
		return factory.create(daoHelper, new FieldJoin(daoHelper.getDomainClass(), HibProjectImpl.class, "project"));
	}

	@Provides
	RootDaoHelper<Schema, HibSchemaImpl, Project, HibProjectImpl> schemaRoot(RootDaoHelperFactory factory, DaoHelperFactory daoHelperFactory) {
		DaoHelper<Schema, HibSchemaImpl> daoHelper = schema(daoHelperFactory);
		return factory.create(daoHelper, new CrossTableJoin(daoHelper.getDomainClass(), HibProjectImpl.class, "project_schema", "projects", "schemas"));
	}

	@Provides
	RootDaoHelper<Microschema, HibMicroschemaImpl, Project, HibProjectImpl> microSchemaRoot(RootDaoHelperFactory factory, DaoHelperFactory daoHelperFactory) {
		DaoHelper<Microschema, HibMicroschemaImpl> daoHelper = microschema(daoHelperFactory);
		return factory.create(daoHelper, new CrossTableJoin(daoHelper.getDomainClass(), HibProjectImpl.class, "project_microschema", "projects", "microschemas"));
	}
}
