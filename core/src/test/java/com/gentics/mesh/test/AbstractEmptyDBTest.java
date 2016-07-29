package com.gentics.mesh.test;

import org.junit.After;
import org.junit.Before;

import com.gentics.mesh.cli.BootstrapInitializer;
import com.gentics.mesh.graphdb.NoTx;

/**
 * @deprecated Use {@link AbstractDBTest} instead
 */
@Deprecated
public class AbstractEmptyDBTest extends AbstractDBTest {

	protected NoTx tx;

	@Before
	public void setup() throws Exception {
		tx = db.noTx();
	}

	@After
	public void cleanup() throws Exception {
		tx.close();
		BootstrapInitializer.clearReferences();
		databaseService.getDatabase().reset();
	}

}
