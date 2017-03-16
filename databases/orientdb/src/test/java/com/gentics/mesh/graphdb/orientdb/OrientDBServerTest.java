package com.gentics.mesh.graphdb.orientdb;

import java.io.File;
import java.io.IOException;

import org.apache.commons.io.FileUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

import com.gentics.mesh.etc.config.GraphStorageOptions;
import com.gentics.mesh.graphdb.OrientDBDatabase;
import com.gentics.mesh.graphdb.Tx;
import com.gentics.mesh.graphdb.orientdb.graph.Person;
import com.gentics.mesh.graphdb.spi.Database;

import io.vertx.core.Vertx;

@Ignore
public class OrientDBServerTest {

	private Database db = new OrientDBDatabase();

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
		GraphStorageOptions options = new GraphStorageOptions();

		options.setDirectory(dbDirectory.getAbsolutePath());
		options.setStartServer(true);
		db.init(options, Vertx.vertx());
		db.start();

		for (int i = 0; i < 100; i++) {
			try (Tx tx = db.tx()) {
				Person p = tx.getGraph().addFramedVertex(Person.class);
				p.setName("personName_" + i);
				tx.success();
				Thread.sleep(5000);
			}
		}
		Thread.sleep(610000);
		db.stop();
	}

}
