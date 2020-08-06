package com.syncleus.ferma.ext.orientdb3;

import com.gentics.mesh.cli.BootstrapInitializer;
import com.gentics.mesh.core.data.dao.ProjectDao;
import com.gentics.mesh.core.data.dao.ProjectDaoWrapper;
import com.gentics.mesh.core.data.job.JobRoot;
import com.gentics.mesh.core.data.root.GroupRoot;
import com.gentics.mesh.core.data.root.LanguageRoot;
import com.gentics.mesh.core.data.root.MicroschemaContainerRoot;
import com.gentics.mesh.core.data.root.RoleRoot;
import com.gentics.mesh.core.data.root.SchemaContainerRoot;
import com.gentics.mesh.core.data.root.TagFamilyRoot;
import com.gentics.mesh.core.data.root.TagRoot;
import com.gentics.mesh.core.data.root.UserRoot;
import com.gentics.mesh.core.db.TxData;

public class OrientTxData implements TxData {
	private final BootstrapInitializer boot;

	public OrientTxData(BootstrapInitializer boot) {
		this.boot = boot;
	}

	@Override
	public UserRoot userDao() {
		return boot.userRoot();
	}

	@Override
	public GroupRoot groupDao() {
		return boot.groupRoot();
	}

	@Override
	public RoleRoot roleDao() {
		return boot.roleRoot();
	}

	@Override
	public ProjectDaoWrapper projectDao() {
		return boot.projectDao();
	}

	@Override
	public JobRoot jobDao() {
		return boot.jobRoot();
	}

	@Override
	public LanguageRoot languageDao() {
		return boot.languageRoot();
	}

	@Override
	public SchemaContainerRoot schemaDao() {
		return boot.schemaContainerRoot();
	}

	@Override
	public TagRoot tagDao() {
		return boot.tagRoot();
	}

	@Override
	public TagFamilyRoot tagFamilyDao() {
		return boot.tagFamilyRoot();
	}

	@Override
	public MicroschemaContainerRoot microschemaDao() {
		return boot.microschemaContainerRoot();
	}
}
