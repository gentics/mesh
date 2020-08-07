package com.syncleus.ferma.ext.orientdb3;

import com.gentics.mesh.cli.BootstrapInitializer;
import com.gentics.mesh.core.data.dao.BinaryDaoWrapper;
import com.gentics.mesh.core.data.dao.BranchDaoWrapper;
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
	private final BootstrapInitializer boot;

	public OrientTxData(BootstrapInitializer boot) {
		this.boot = boot;
	}

	@Override
	public UserDaoWrapper userDao() {
		return boot.userDao();
	}

	@Override
	public GroupDaoWrapper groupDao() {
		return boot.groupDao();
	}

	@Override
	public RoleDaoWrapper roleDao() {
		return boot.roleDao();
	}

	@Override
	public ProjectDaoWrapper projectDao() {
		return boot.projectDao();
	}

	@Override
	public JobDaoWrapper jobDao() {
		return boot.jobDao();
	}

	@Override
	public LanguageDaoWrapper languageDao() {
		return boot.languageDao();
	}

	@Override
	public SchemaDaoWrapper schemaDao() {
		return boot.schemaDao();
	}

	@Override
	public TagDaoWrapper tagDao() {
		return boot.tagDao();
	}

	@Override
	public TagFamilyDaoWrapper tagFamilyDao() {
		return boot.tagFamilyDao();
	}

	@Override
	public MicroschemaDaoWrapper microschemaDao() {
		return boot.microschemaDao();
	}
	
	@Override
	public BinaryDaoWrapper binaryDao() {
		//TODO There is currently no binary wrapper. We need to write a new impl.
		return null;
	}
	
	@Override
	public BranchDaoWrapper branchDao() {
		// TODO there is currently no global branch root
		return null;
	}
}
