package com.gentics.mesh.graphdb.orientdb;

import org.junit.Before;
import org.junit.Test;

import com.syncleus.ferma.DelegatingFramedTransactionalGraph;
import com.syncleus.ferma.FramedTransactionalGraph;
import com.tinkerpop.blueprints.impls.orient.OrientGraph;
import com.tinkerpop.blueprints.impls.orient.OrientGraphFactory;
import com.tinkerpop.blueprints.impls.orient.OrientTransactionalGraph;

public class OrientDBFermaMultithreadingReducedTest extends AbstractOrientDBTest {

	OrientTransactionalGraph memoryGraph = new OrientGraph("memory:tinkerpop");
	OrientGraphFactory factory = new OrientGraphFactory("memory:tinkerpop");//.setupPool(5, 100);

	FramedTransactionalGraph fg;

	@Before
	public void setup() {
		fg = new DelegatingFramedTransactionalGraph<>(factory.getTx(), true, false);
	}

	@Test
	public void testMultithreading() {
		runAndWait(() -> {
			Person p = addPersonWithFriends(fg);
			p.setName("joe");
			runAndWait(() -> {
				manipulatePerson(p);
			});
		});
	}

}
