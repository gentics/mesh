package com.gentics.mesh.core.data;

import org.junit.After;
import org.junit.Before;

import com.gentics.mesh.graphdb.NoTx;
import com.gentics.mesh.test.AbstractDBTest;

/**
 * @deprecated Use {@link AbstractIsolatedBasicDBTest} instead.
 *
 */
@Deprecated
public class AbstractBasicDBTest extends AbstractDBTest {

	protected NoTx tx;

	@Before
	public void initDagger() throws Exception {
		super.initDagger();
		setupData();
		tx = db.noTx();
	}

	@After
	public void cleanup() {
		if (tx != null) {
			tx.close();
		}
		resetDatabase();
	}

}
