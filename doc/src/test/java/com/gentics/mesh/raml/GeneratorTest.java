package com.gentics.mesh.raml;

import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.IOException;

import org.apache.commons.io.FileUtils;
import org.junit.Before;
import org.junit.Test;

import com.gentics.mesh.raml.Generator;

public class GeneratorTest {
	private File baseDir = new File("target", "raml2html");

	@Before
	public void setup() throws IOException {
		FileUtils.deleteDirectory(baseDir);
	}

	@Test
	public void testGenerator() throws IOException {
		File jsonDir = new File(baseDir, "json");
		new Generator().start();
		assertTrue(jsonDir.exists());
		assertTrue(jsonDir.listFiles().length != 0);
	}
}
