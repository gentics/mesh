package com.gentics.mesh.core.field.bool;

import org.junit.After;
import org.junit.Before;

import com.gentics.mesh.cli.BootstrapInitializer;
import com.gentics.mesh.graphdb.NoTrx;
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
		// databaseService.getDatabase().clear();
		databaseService.getDatabase().reset();
	}

}
