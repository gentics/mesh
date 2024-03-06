package com.gentics.mesh.graphdb.orientdb;

import static com.gentics.mesh.graphdb.orientdb.ThreadUtils.runAndWait;

import org.apache.tinkerpop.gremlin.orientdb.OrientGraph;
import org.apache.tinkerpop.gremlin.orientdb.OrientGraphFactory;
import org.apache.tinkerpop.gremlin.orientdb.OrientVertex;
import org.apache.tinkerpop.gremlin.structure.Vertex;
import org.junit.Test;


public class OrientDBTinkerpopMultithreadingTest extends AbstractOrientDBTest {

	OrientGraphFactory factory = new OrientGraphFactory("memory:tinkerpop");

	@Test
	public void testMultithreading() {
		OrientGraph graph = factory.getTx();
		Vertex v2 = graph.addVertex();
		Vertex v = graph.addVertex();
		graph.commit();
		
		//		Object id = v.getId();
		runAndWait(() -> {
			graph.getRawDatabase().activateOnCurrentThread();
			Vertex v3 = new OrientVertex(graph, ((OrientVertex) v).getRawElement());
			v3.property("sfaf", "dxgvasdg");
			v3.addEdge("adadsg", v2);
			//			Vertex e = memoryGraph.getVertex(id);
			//			assertNotNull(e);
			//			assertEquals("marko", e.getProperty("name"));
			//			e.setProperty("name", "joe");
			//			memoryGraph.rollback();
		});
		//		assertEquals("marko", v.getProperty("name"));

	}

}
