package com.gentics.mesh.graphdb.orientdb;

import java.io.File;
import java.io.IOException;

import org.apache.commons.io.FileUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

import com.gentics.mesh.etc.config.AbstractMeshOptions;
import com.gentics.mesh.graphdb.orientdb.graph.Person;
import com.gentics.mesh.graphdb.spi.Database;

@Ignore
public class OrientDBServerTest extends AbstractOrientDBTest {


	private File dbDirectory;

	@Before
	public void createTmpDir() {
		dbDirectory = new File(System.getProperty("java.io.tmpdir"), "random_" + Math.random());
		dbDirectory.mkdirs();
	}

	@After
	public void cleanup() throws IOException {
		FileUtils.deleteDirectory(dbDirectory);
	}

	@Test
	public void testServer() throws Exception {
		AbstractMeshOptions options = new AbstractMeshOptions();
		options.getStorageOptions().setDirectory(dbDirectory.getAbsolutePath());
		options.getStorageOptions().setStartServer(true);

		Database db = mockDatabase(options);
		db.init(null);
		db.setupConnectionPool();

		for (int i = 0; i < 100; i++) {
			int e = i;
			db.tx(tx -> {
				Person p = tx.getGraph().addFramedVertex(Person.class);
				p.setName("personName_" + e);
				tx.success();
				Thread.sleep(5000);
			});
		}
		Thread.sleep(610000);
		db.stop();
	}

}
