package com.gentics.mesh.graphdb.orientdb;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import org.junit.Before;
import org.junit.Test;

import com.tinkerpop.blueprints.Vertex;
import com.tinkerpop.blueprints.impls.orient.OrientGraph;
import com.tinkerpop.blueprints.impls.orient.OrientTransactionalGraph;

public class OrientDBTinkerpopMultithreadingTest {

	OrientTransactionalGraph memoryGraph = new OrientGraph("memory:tinkerpop");

	@Before
	public void cleanup() {

	}

	@Test
	public void testMultithreading() {

		Vertex v = memoryGraph.addVertex(null);
		memoryGraph.commit();
		// Other code
		v = memoryGraph.getVertex(v.getId());
		assertNotNull(v);
		v.setProperty("name", "marko");
		assertEquals("marko", v.getProperty("name"));
		memoryGraph.commit();

		Object id = v.getId();
		runAndWait(() -> {
			Vertex e = memoryGraph.getVertex(id);
			assertNotNull(e);
			assertEquals("marko", e.getProperty("name"));
			e.setProperty("name", "joe");
			memoryGraph.rollback();
		});
		assertEquals("marko", v.getProperty("name"));

	}

	public void runAndWait(Runnable runnable) {
		Thread thread = new Thread(runnable);
		thread.start();
		try {
			thread.join();
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		System.out.println("Done waiting");
	}

}
