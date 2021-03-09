package com.gentics.mesh.core.data.dao;

import javax.inject.Inject;
import javax.inject.Singleton;

import com.gentics.mesh.core.action.BranchDAOActions;
import com.gentics.mesh.core.action.GroupDAOActions;
import com.gentics.mesh.core.action.MicroschemaDAOActions;
import com.gentics.mesh.core.action.ProjectDAOActions;
import com.gentics.mesh.core.action.RoleDAOActions;
import com.gentics.mesh.core.action.SchemaDAOActions;
import com.gentics.mesh.core.action.TagDAOActions;
import com.gentics.mesh.core.action.TagFamilyDAOActions;
import com.gentics.mesh.core.action.UserDAOActions;
import com.gentics.mesh.core.context.ContextDataRegistry;

@Singleton
public class OrientDBDaoCollection implements DaoCollection {

	private final OrientDBUserDao userDao;
	private final UserDAOActions userActions;

	private final OrientDBGroupDao groupDao;
	private final GroupDAOActions groupActions;

	private final OrientDBRoleDao roleDao;
	private final RoleDAOActions roleActions;

	private final OrientDBTagDao tagDao;
	private final TagDAOActions tagActions;

	private final OrientDBTagFamilyDao tagFamilyDao;
	private final TagFamilyDAOActions tagFamilyActions;

	private final OrientDBProjectDao projectDao;
	private final ProjectDAOActions projectActions;

	private final OrientDBBranchDao branchDao;
	private final BranchDAOActions branchActions;

	private final OrientDBSchemaDao schemaDao;
	private final SchemaDAOActions schemaActions;

	private final OrientDBMicroschemaDao microschemaDao;
	private final MicroschemaDAOActions microschemaActions;

	private final OrientDBNodeDao nodeDao;
	private final OrientDBContentDao contentDao;

	private final OrientDBLanguageDao languageDao;
	private final OrientDBBinaryDao binaryDao;
	private final OrientDBJobDao jobDao;

	@Inject
	public OrientDBDaoCollection(
		OrientDBUserDao userDao,
		UserDAOActions userActions,

		OrientDBGroupDao groupDao,
		GroupDAOActions groupActions,

		OrientDBRoleDao roleDao,
		RoleDAOActions roleActions,

		OrientDBProjectDao projectDao,
		ProjectDAOActions projectActions,

		OrientDBTagFamilyDao tagFamilyDao,
		TagFamilyDAOActions tagFamilyActions,

		OrientDBTagDao tagDao,
		TagDAOActions tagActions,

		OrientDBBranchDao branchDao,
		BranchDAOActions branchActions,

		OrientDBSchemaDao schemaDao,
		SchemaDAOActions schemaActions,

		OrientDBMicroschemaDao microschemaDao,
		MicroschemaDAOActions microschemaActions,

		OrientDBNodeDao nodeDao,
		OrientDBContentDao contentDao,

		OrientDBLanguageDao languageDao,
		OrientDBBinaryDao binaryDao,
		OrientDBJobDao jobDao,

		ContextDataRegistry contextDataRegistry) {
		this.userDao = userDao;
		this.userActions = userActions;

		this.groupDao = groupDao;
		this.groupActions = groupActions;

		this.roleDao = roleDao;
		this.roleActions = roleActions;

		this.tagDao = tagDao;
		this.tagActions = tagActions;

		this.tagFamilyDao = tagFamilyDao;
		this.tagFamilyActions = tagFamilyActions;

		this.branchDao = branchDao;
		this.branchActions = branchActions;

		this.schemaDao = schemaDao;
		this.schemaActions = schemaActions;

		this.microschemaDao = microschemaDao;
		this.microschemaActions = microschemaActions;

		this.nodeDao = nodeDao;
		this.contentDao = contentDao;

		this.projectDao = projectDao;
		this.projectActions = projectActions;

		this.languageDao = languageDao;
		this.binaryDao = binaryDao;
		this.jobDao = jobDao;
	}

	@Override
	public OrientDBUserDao userDao() {
		return userDao;
	}

	@Override
	public UserDAOActions userActions() {
		return userActions;
	}

	@Override
	public OrientDBGroupDao groupDao() {
		return groupDao;
	}

	@Override
	public GroupDAOActions groupActions() {
		return groupActions;
	}

	@Override
	public OrientDBRoleDao roleDao() {
		return roleDao;
	}

	@Override
	public RoleDAOActions roleActions() {
		return roleActions;
	}

	@Override
	public OrientDBProjectDao projectDao() {
		return projectDao;
	}

	@Override
	public ProjectDAOActions projectActions() {
		return projectActions;
	}

	@Override
	public OrientDBLanguageDao languageDao() {
		return languageDao;
	}

	@Override
	public OrientDBJobDao jobDao() {
		return jobDao;
	}

	@Override
	public OrientDBTagFamilyDao tagFamilyDao() {
		return tagFamilyDao;
	}

	@Override
	public TagFamilyDAOActions tagFamilyActions() {
		return tagFamilyActions;
	}

	@Override
	public OrientDBTagDao tagDao() {
		return tagDao;
	}

	@Override
	public TagDAOActions tagActions() {
		return tagActions;
	}

	@Override
	public OrientDBBranchDao branchDao() {
		return branchDao;
	}

	@Override
	public BranchDAOActions branchActions() {
		return branchActions;
	}

	@Override
	public OrientDBMicroschemaDao microschemaDao() {
		return microschemaDao;
	}

	@Override
	public MicroschemaDAOActions microschemaActions() {
		return microschemaActions;
	}

	@Override
	public OrientDBSchemaDao schemaDao() {
		return schemaDao;
	}

	@Override
	public SchemaDAOActions schemaActions() {
		return schemaActions;
	}

	@Override
	public OrientDBBinaryDao binaryDao() {
		return binaryDao;
	}

	@Override
	public OrientDBNodeDao nodeDao() {
		return nodeDao;
	}

	@Override
	public OrientDBContentDao contentDao() {
		return contentDao;
	}

}
