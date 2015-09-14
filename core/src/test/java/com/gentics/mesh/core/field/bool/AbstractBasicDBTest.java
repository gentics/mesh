package com.gentics.mesh.core.field.bool;

import org.junit.After;
import org.junit.Before;

import com.gentics.mesh.graphdb.NonTrx;
import com.gentics.mesh.test.AbstractDBTest;

public class AbstractBasicDBTest extends AbstractDBTest {

	protected NonTrx tx;

	@Before
	public void setup() throws Exception {
		tx = db.nonTrx();
		setupData();
	}

	@After
	public void after() {
		tx.close();
	}

}
