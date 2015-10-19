package com.gentics.mesh.core.field.bool;

import org.junit.After;
import org.junit.Before;

import com.gentics.mesh.cli.BootstrapInitializer;
import com.gentics.mesh.core.data.impl.LanguageImpl;
import com.gentics.mesh.core.data.impl.RoleImpl;
import com.gentics.mesh.core.data.root.impl.GroupRootImpl;
import com.gentics.mesh.core.data.root.impl.LanguageRootImpl;
import com.gentics.mesh.core.data.root.impl.NodeRootImpl;
import com.gentics.mesh.core.data.root.impl.RoleRootImpl;
import com.gentics.mesh.core.data.root.impl.TagFamilyRootImpl;
import com.gentics.mesh.core.data.root.impl.TagRootImpl;
import com.gentics.mesh.core.data.root.impl.UserRootImpl;
import com.gentics.mesh.etc.MeshSpringConfiguration;
import com.gentics.mesh.graphdb.NoTrx;
import com.gentics.mesh.graphdb.spi.Database;
import com.gentics.mesh.test.AbstractDBTest;

public class AbstractBasicDBTest extends AbstractDBTest {

	protected NoTrx tx;

	@Before
	public void setup() throws Exception {
		setupData();
		tx = db.noTrx();
	}

	@After
	public void cleanup() {
		tx.close();
		BootstrapInitializer.clearReferences();
		databaseService.getDatabase().clear();
		Database database = MeshSpringConfiguration.getInstance().database();
		GroupRootImpl.checkIndices(database);
		UserRootImpl.checkIndices(database);
		RoleRootImpl.checkIndices(database);
		TagRootImpl.checkIndices(database);
		NodeRootImpl.checkIndices(database);
		TagFamilyRootImpl.checkIndices(database);
		RoleImpl.checkIndices(database);
		LanguageRootImpl.checkIndices(database);
		LanguageImpl.checkIndices(database);

		//databaseService.getDatabase().reset();
	}

}
