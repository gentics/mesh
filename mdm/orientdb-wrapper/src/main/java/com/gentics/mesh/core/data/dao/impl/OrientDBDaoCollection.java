package com.gentics.mesh.core.data.dao.impl;

import javax.inject.Inject;
import javax.inject.Singleton;

import com.gentics.mesh.core.data.dao.BinaryDaoWrapper;
import com.gentics.mesh.core.data.dao.BranchDaoWrapper;
import com.gentics.mesh.core.data.dao.DaoCollection;
import com.gentics.mesh.core.data.dao.GroupDaoWrapper;
import com.gentics.mesh.core.data.dao.JobDaoWrapper;
import com.gentics.mesh.core.data.dao.LanguageDaoWrapper;
import com.gentics.mesh.core.data.dao.MicroschemaDaoWrapper;
import com.gentics.mesh.core.data.dao.ProjectDaoWrapper;
import com.gentics.mesh.core.data.dao.RoleDaoWrapper;
import com.gentics.mesh.core.data.dao.SchemaDaoWrapper;
import com.gentics.mesh.core.data.dao.TagDaoWrapper;
import com.gentics.mesh.core.data.dao.TagFamilyDaoWrapper;
import com.gentics.mesh.core.data.dao.UserDaoWrapper;

@Singleton
public class OrientDBDaoCollection implements DaoCollection {

	private final UserDaoWrapper userDao;
	private final GroupDaoWrapper groupDao;
	private final RoleDaoWrapper roleDao;
	private final TagDaoWrapper tagDao;
	private final TagFamilyDaoWrapper tagFamilyDao;
	private final BranchDaoWrapper branchDao;
	private final BinaryDaoWrapper binaryDao;
	private final SchemaDaoWrapper schemaDao;
	private final MicroschemaDaoWrapper microschemaDao;
	private final ProjectDaoWrapper projectDao;
	private final LanguageDaoWrapper languageDao;
	private JobDaoWrapper jobDao;

	@Inject
	public OrientDBDaoCollection(
		RoleDaoWrapper roleDao,
		GroupDaoWrapper groupDao,
		UserDaoWrapper userDao,
		ProjectDaoWrapper projectDao,
		TagFamilyDaoWrapper tagFamilyDao,
		TagDaoWrapper tagDao,
		BranchDaoWrapper branchDao,
		BinaryDaoWrapper binaryDao,
		SchemaDaoWrapper schemaDao,
		MicroschemaDaoWrapper microschemaDao,
		LanguageDaoWrapper languageDao,
		JobDaoWrapper jobDao) {
		this.userDao = userDao;
		this.groupDao = groupDao;
		this.roleDao = roleDao;
		this.tagDao = tagDao;
		this.tagFamilyDao = tagFamilyDao;
		this.branchDao = branchDao;
		this.binaryDao = binaryDao;
		this.schemaDao = schemaDao;
		this.microschemaDao = microschemaDao;
		this.projectDao = projectDao;
		this.languageDao = languageDao;
		this.jobDao = jobDao;
	}

	@Override
	public UserDaoWrapper userDao() {
		return userDao;
	}

	@Override
	public GroupDaoWrapper groupDao() {
		return groupDao;
	}

	@Override
	public RoleDaoWrapper roleDao() {
		return roleDao;
	}

	@Override
	public ProjectDaoWrapper projectDao() {
		return projectDao;
	}

	@Override
	public LanguageDaoWrapper languageDao() {
		return languageDao;
	}

	@Override
	public JobDaoWrapper jobDao() {
		return jobDao;
	}

	@Override
	public TagFamilyDaoWrapper tagFamilyDao() {
		return tagFamilyDao;
	}

	@Override
	public TagDaoWrapper tagDao() {
		return tagDao;
	}

	@Override
	public BranchDaoWrapper branchDao() {
		return branchDao;
	}

	@Override
	public MicroschemaDaoWrapper microschemaDao() {
		return microschemaDao;
	}

	@Override
	public SchemaDaoWrapper schemaDao() {
		return schemaDao;
	}

	@Override
	public BinaryDaoWrapper binaryDao() {
		return binaryDao;
	}
}
