package com.gentics.mesh.test;

import org.junit.After;
import org.junit.Before;

import com.gentics.mesh.cli.BootstrapInitializer;
import com.gentics.mesh.graphdb.NoTrx;

public class AbstractEmptyDBTest extends AbstractDBTest {

	protected NoTrx tx;

	@Before
	public void setup() throws Exception {
		tx = db.noTrx();
	}

	@After
	public void cleanup() {
		tx.close();
		BootstrapInitializer.clearReferences();
		databaseService.getDatabase().reset();
	}

}
