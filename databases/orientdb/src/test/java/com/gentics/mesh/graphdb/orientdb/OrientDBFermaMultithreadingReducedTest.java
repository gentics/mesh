package com.gentics.mesh.graphdb.orientdb;

import static org.junit.Assert.assertNotNull;

import org.junit.Before;
import org.junit.Test;

import com.gentics.mesh.graphdb.BlueprintTransaction;
import com.gentics.mesh.graphdb.OrientThreadedTransactionalGraphWrapper;
import com.gentics.mesh.graphdb.ThreadedTransactionalGraphWrapper;
import com.syncleus.ferma.DelegatingFramedThreadedTransactionalGraph;
import com.syncleus.ferma.FramedThreadedTransactionalGraph;
import com.tinkerpop.blueprints.impls.orient.OrientGraphFactory;

public class OrientDBFermaMultithreadingReducedTest extends AbstractOrientDBTest {

	OrientGraphFactory factory = new OrientGraphFactory("memory:tinkerpop");//.setupPool(5, 100);

	FramedThreadedTransactionalGraph fg;

	@Before
	public void setup() {
		ThreadedTransactionalGraphWrapper wrapper = new OrientThreadedTransactionalGraphWrapper(factory);
		fg = new DelegatingFramedThreadedTransactionalGraph<>(wrapper, true, false);
	}

	@Test
	public void testMultithreading() {
		String name = "SomeName";
		Person p = addPersonWithFriends(fg, name);
		System.out.println(p.getGraph().getClass().getName());
		fg.commit();
		runAndWait(() -> {
//			OrientGraph graph2 = factory.getTx();
//			graph2.getRawGraph().activateOnCurrentThread();
			try (BlueprintTransaction tx = new BlueprintTransaction(fg)) {
				//			FramedTransactionalGraph fg2 = new DelegatingFramedTransactionalGraph<>(graph2, true, false);
				//graph.attach((OrientElement) p.getElement());
				manipulatePerson(p);
				p.setName(name);

				// Reload example
				//			for (VertexFrame vertex : fg.v().toList()) {
				//				System.out.println(vertex.toString());
				//			}
				Person reloaded = fg.v().has(Person.class).has("name", name).nextOrDefaultExplicit(Person.class, null);
				System.out.println(reloaded.getName());
				assertNotNull(reloaded);
				manipulatePerson(reloaded);
			}
		});
	}

}
