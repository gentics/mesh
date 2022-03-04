package com.gentics.mesh.demo;

import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.IOException;

import org.apache.commons.io.FileUtils;
import org.junit.After;
import org.junit.Test;

public class UnzipTest {

	@Test
	public void testUnzip() throws Exception {
		DemoZipHelper.unzip("/mesh-demo.zip", "target/demo");
		assertTrue(new File("target/demo").exists());
	}

	@After
	public void cleanup() throws IOException {
		FileUtils.deleteDirectory(new File("target/demo"));
	}
}
