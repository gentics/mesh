package com.syncleus.ferma.ext.orientdb3;

import com.gentics.mesh.cli.BootstrapInitializer;
import com.gentics.mesh.core.data.root.ProjectRoot;
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
	public ProjectRoot projectDao() {
		return boot.projectRoot();
	}
}
