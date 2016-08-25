package com.gentics.mesh.core.data;

import org.junit.After;
import org.junit.Before;

import com.gentics.mesh.test.AbstractDBTest;

public class AbstractIsolatedBasicDBTest extends AbstractDBTest {

	@Before
	public void setup() throws Exception {
		super.setup();
		setupData();
	}

	@After
	public void cleanup() {
		resetDatabase();
	}

}
