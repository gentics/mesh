package com.gentics.mesh.graphdb.arcadedb;

import static com.gentics.mesh.graphdb.arcadedb.ThreadUtils.runAndWait;

import org.apache.tinkerpop.gremlin.structure.Vertex;
import org.junit.Test;

import com.arcadedb.gremlin.ArcadeGraph;
import com.arcadedb.gremlin.ArcadeGraphFactory;
import com.arcadedb.gremlin.ArcadeVertex;

public class ArcadeDBTinkerpopMultithreadingTest extends AbstractArcadeDBTest {

	ArcadeGraphFactory factory = ArcadeGraphFactory.withLocal("memory:tinkerpop");

	@Test
	public void testMultithreading() {
		ArcadeGraph graph = factory.get();
		Vertex v2 = graph.addVertex();
		Vertex v = graph.addVertex();
		graph.tx().commit();
		
		//		Object id = v.getId();
		runAndWait(() -> {
			Vertex v3 = v;
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
