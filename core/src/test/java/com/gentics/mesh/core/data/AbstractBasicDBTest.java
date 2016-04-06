package com.gentics.mesh.core.data;

import org.junit.After;
import org.junit.Before;

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
		if (tx != null) {
			tx.close();
		}
		resetDatabase();
	}

}
