package com.gentics.mesh.graphdb;

import static com.gentics.mesh.test.TestSize.FULL;
import static org.junit.Assert.assertEquals;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;

import org.apache.commons.io.FileUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.gentics.mesh.Mesh;
import com.gentics.mesh.core.data.impl.LanguageImpl;
import com.gentics.mesh.core.data.relationship.GraphRelationships;
import com.gentics.mesh.error.MeshSchemaException;
import com.gentics.mesh.graphdb.spi.FieldType;
import com.gentics.mesh.test.context.AbstractMeshTest;
import com.gentics.mesh.test.context.MeshTestSetting;
import com.gentics.madl.tx.Tx;

@MeshTestSetting(useElasticsearch = false, testSize = FULL, startServer = false)
public class DatabaseTest extends AbstractMeshTest {

	private File outputDirectory;

	@Before
	public void createOutputDirectory() throws JsonParseException, JsonMappingException, IOException, MeshSchemaException {
		outputDirectory = new File("target", "tmp_" + System.currentTimeMillis());
		outputDirectory.mkdirs();
		Mesh.mesh().getOptions().getStorageOptions().setDirectory(new File(outputDirectory, "graphdb").getAbsolutePath());
		// db().reset();
		// setupData();
	}

	@After
	public void removeOutputDir() throws IOException {
		FileUtils.deleteDirectory(outputDirectory);
	}

	@Test
	public void testIndex() {
		try (Tx tx = tx()) {
			db().addVertexIndex(LanguageImpl.class, true, "languageTag", FieldType.STRING);
			db().addEdgeIndex(GraphRelationships.ASSIGNED_TO_ROLE, false, false, true);
		}
	}

	@Test
	public void testExport() throws IOException {
		db().exportGraph(outputDirectory.getAbsolutePath());
	}

	@Test
	public void testImport() throws IOException {
		db().exportGraph(outputDirectory.getAbsolutePath());
		File[] fileArray = outputDirectory.listFiles();
		if (fileArray != null) {
			List<File> files = Arrays.asList(fileArray);
			File importFile = files.iterator().next();
			db().importGraph(importFile.getAbsolutePath());
		}
	}

	@Test
	@Ignore
	public void testBackup() throws IOException {
		db().backupGraph(outputDirectory.getAbsolutePath());
	}

	@Test
	@Ignore
	public void testRestore() throws IOException {
		String name = "username";
		try (Tx tx = tx()) {
			user().setUsername(name);
			tx.success();
		}
		try (Tx tx = tx()) {
			assertEquals(name, user().getUsername());
		}
		db().backupGraph(outputDirectory.getAbsolutePath());
		try (Tx tx = tx()) {
			user().setUsername("changed");
			tx.success();
		}
		try (Tx tx = tx()) {
			assertEquals("changed", user().getUsername());
		}
		db().restoreGraph(outputDirectory.listFiles()[0].getAbsolutePath());
		try (Tx tx = tx()) {
			assertEquals("username", user().getUsername());
		}

	}

}
