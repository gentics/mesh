package com.gentics.mesh.core;

import static org.junit.Assert.assertEquals;

import java.io.File;
import java.io.IOException;

import org.apache.commons.io.FileUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.gentics.mesh.cli.Mesh;
import com.gentics.mesh.error.MeshSchemaException;
import com.gentics.mesh.graphdb.Trx;
import com.gentics.mesh.test.AbstractDBTest;

public class DatabaseTest extends AbstractDBTest {

	private File outputDirectory;

	@Before
	public void createOutputDirectory() throws JsonParseException, JsonMappingException, IOException, MeshSchemaException {
		outputDirectory = new File("target", "tmp_" + System.currentTimeMillis());
		outputDirectory.mkdirs();
		Mesh.mesh().getOptions().getStorageOptions().setDirectory(new File(outputDirectory, "graphdb").getAbsolutePath());
		db.reset();
		setupData();
	}

	@After
	public void cleanup() throws IOException {
		FileUtils.deleteDirectory(outputDirectory);
	}

	@Test
	public void testExport() throws IOException {
		db.exportGraph(outputDirectory.getAbsolutePath());
	}

	@Test
	public void testImport() throws IOException {
		db.exportGraph(outputDirectory.getAbsolutePath());
		db.importGraph(outputDirectory.listFiles()[0].getAbsolutePath());
	}

	@Test
	public void testBackup() throws IOException {
		db.backupGraph(outputDirectory.getAbsolutePath());
	}

	@Test
	@Ignore
	public void testRestore() throws IOException {
		String name = "username";
		try (Trx tx = db.trx()) {
			user().setUsername(name);
			tx.success();
		}
		try (Trx tx = db.trx()) {
			assertEquals(name, user().getUsername());
		}
		db.backupGraph(outputDirectory.getAbsolutePath());
		try (Trx tx = db.trx()) {
			user().setUsername("changed");
			tx.success();
		}
		try (Trx tx = db.trx()) {
			assertEquals("changed", user().getUsername());
		}
		db.restoreGraph(outputDirectory.listFiles()[0].getAbsolutePath());
		try (Trx tx = db.trx()) {
			assertEquals("username", user().getUsername());
		}

	}

}
