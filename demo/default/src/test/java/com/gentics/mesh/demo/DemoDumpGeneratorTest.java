package com.gentics.mesh.demo;

import org.junit.BeforeClass;

public class DemoDumpGeneratorTest extends AbstractDemoDumperTest {

	static {
		generator = new DemoDumpGenerator(new String[0]);
	}

	@BeforeClass
	public static void cleanupFolders() throws Exception {
		generator.cleanup();
		generator.init();
	}
}
