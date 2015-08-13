package com.gentics.mesh.graphdb.orientdb;

import org.junit.Test;

import com.tinkerpop.blueprints.Vertex;
import com.tinkerpop.blueprints.impls.orient.OrientGraph;
import com.tinkerpop.blueprints.impls.orient.OrientTransactionalGraph;

public class OrientDBTinkerpopMultithreadingTest extends AbstractOrientDBTest {

	OrientTransactionalGraph memoryGraph = new OrientGraph("memory:tinkerpop");

	@Test
	public void testMultithreading() {
		Vertex v2 = memoryGraph.addVertex(null);
		Vertex v = memoryGraph.addVertex(null);
		memoryGraph.commit();
		//		Object id = v.getId();
		runAndWait(() -> {
//			memoryGraph.activateOnCurrentThread(); 
			v.setProperty("sfaf", "dxgvasdg");
			v.addEdge("adadsg", v2);
			//			Vertex e = memoryGraph.getVertex(id);
			//			assertNotNull(e);
			//			assertEquals("marko", e.getProperty("name"));
			//			e.setProperty("name", "joe");
			//			memoryGraph.rollback();
		});
		//		assertEquals("marko", v.getProperty("name"));

	}

}
