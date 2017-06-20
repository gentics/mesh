package com.gentics.mesh.distributed;

import java.io.File;

import org.apache.commons.io.FileUtils;
import org.junit.Ignore;
import org.junit.Test;

import com.orientechnologies.orient.core.Orient;
import com.orientechnologies.orient.core.db.ODatabase;
import com.orientechnologies.orient.core.metadata.security.OSecurityNull;
import com.tinkerpop.blueprints.impls.orient.OrientGraphFactory;
import com.tinkerpop.blueprints.impls.orient.OrientGraphNoTx;

@Ignore
public class ReplicationTest {

	String basePath = "target/nodeC";
	private final String nodeName = "nodeC";
	protected TestDatabase db;

	public void startOrientServer(String name) throws Exception {

		File baseDir = new File(basePath);
		FileUtils.deleteDirectory(baseDir);
		baseDir.mkdirs();

		db = new TestDatabase(name, baseDir);

		// 1. Start the orient server
		Runnable t = () -> {
			try {
				db.startOrientServer();
			} catch (Exception e) {
				e.printStackTrace();
			}
		};
		new Thread(t).start();

		// 2. Let the server startup
		System.out.println("Waiting");
		Thread.sleep(10000);
		System.out.println("Waited");

	}

	@Test
	public void testCluster() throws Exception {
		Orient.instance().startup();
		OrientGraphFactory factory = new OrientGraphFactory("plocal:" + new File(basePath, "db").getAbsolutePath());
		factory.setProperty(ODatabase.OPTIONS.SECURITY.toString(), OSecurityNull.class);
		startOrientServer(nodeName);
		System.in.read();
		while (true) {
			OrientGraphNoTx graph = factory.getNoTx();
			System.out.println("Count: " + graph.countVertices());
			Thread.sleep(1500);
			graph.shutdown();
		}

	}
}
