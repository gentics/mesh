package com.gentics.mesh.test;

import org.junit.BeforeClass;

public abstract class AbstractTinyDBTest extends AbstractDBTest {

	/**
	 * Initialise mesh only once. The dagger context will only be setup once.
	 * 
	 * @throws Exception
	 */
	@BeforeClass
	public static void initMesh() throws Exception {
		init(false);
		initDagger(true);
	}
}
