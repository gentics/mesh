package com.gentics.mesh.graphdb.orientdb;

import org.junit.Before;
import org.junit.Test;

import com.syncleus.ferma.DelegatingFramedTransactionalGraph;
import com.syncleus.ferma.FramedTransactionalGraph;
import com.tinkerpop.blueprints.impls.orient.OrientElement;
import com.tinkerpop.blueprints.impls.orient.OrientGraph;
import com.tinkerpop.blueprints.impls.orient.OrientGraphFactory;

public class OrientDBFermaMultithreadingReducedTest extends AbstractOrientDBTest {

	OrientGraphFactory factory = new OrientGraphFactory("memory:tinkerpop");//.setupPool(5, 100);

	FramedTransactionalGraph fg;
	OrientGraph graph;

	@Before
	public void setup() {
		graph = factory.getTx();
		fg = new DelegatingFramedTransactionalGraph<>(graph, true, false);
	}

	@Test
	public void testMultithreading() {
		Person p = addPersonWithFriends(fg);
		p.setName("joe");
		System.out.println(p.getGraph().getClass().getName());

		runAndWait(() -> {
//			graph.getRawGraph().activateOnCurrentThread();
			graph.attach((OrientElement) p.getElement());
			manipulatePerson(p);
		});
	}

}
