package com.syncleus.ferma.ext.orientdb3;

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
import com.gentics.mesh.core.db.TxData;

public class OrientTxData implements TxData {

	private final DaoCollection daos;

	public OrientTxData(DaoCollection daoCollection) {
		this.daos = daoCollection;
	}

	@Override
	public UserDaoWrapper userDao() {
		return daos.userDao();
	}

	@Override
	public GroupDaoWrapper groupDao() {
		return daos.groupDao();
	}

	@Override
	public RoleDaoWrapper roleDao() {
		return daos.roleDao();
	}

	@Override
	public ProjectDaoWrapper projectDao() {
		return daos.projectDao();
	}

	@Override
	public JobDaoWrapper jobDao() {
		return daos.jobDao();
	}

	@Override
	public LanguageDaoWrapper languageDao() {
		return daos.languageDao();
	}

	@Override
	public SchemaDaoWrapper schemaDao() {
		return daos.schemaDao();
	}

	@Override
	public TagDaoWrapper tagDao() {
		return daos.tagDao();
	}

	@Override
	public TagFamilyDaoWrapper tagFamilyDao() {
		return daos.tagFamilyDao();
	}

	@Override
	public MicroschemaDaoWrapper microschemaDao() {
		return daos.microschemaDao();
	}

	@Override
	public BinaryDaoWrapper binaryDao() {
		return daos.binaryDao();
	}

	@Override
	public BranchDaoWrapper branchDao() {
		return daos.branchDao();
	}
}
