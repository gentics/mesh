package com.gentics.mesh.core.monitoring;

import org.junit.After;
import org.junit.Before;

import com.gentics.mesh.MeshStatus;
import com.gentics.mesh.test.context.AbstractMeshTest;

public abstract class AbstractHealthTest extends AbstractMeshTest {

	/**
	 * Make sure that the status will be reset after the test so that 
	 * the database setup and initial login will not fail.
	 */
	@After
	@Before
	public void setReady() {
		meshApi().setStatus(MeshStatus.READY);
	}

}
