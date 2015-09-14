package com.gentics.mesh.test;

import org.junit.After;
import org.junit.Before;

import com.gentics.mesh.cli.BootstrapInitializer;
import com.gentics.mesh.graphdb.NonTrx;

public class AbstractEmptyDBTest extends AbstractDBTest {

	protected NonTrx tx;

	@Before
	public void setup() throws Exception {
		tx = db.nonTrx();
	}

	@After
	public void cleanup() {
		tx.close();
		BootstrapInitializer.clearReferences();
		databaseService.getDatabase().reset();
	}

}
