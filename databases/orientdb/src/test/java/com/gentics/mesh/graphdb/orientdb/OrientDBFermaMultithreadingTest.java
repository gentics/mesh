package com.gentics.mesh.graphdb.orientdb;

import java.util.concurrent.atomic.AtomicReference;

import org.junit.Before;
import org.junit.Test;

import com.gentics.mesh.graphdb.BlueprintTransaction;
import com.gentics.mesh.graphdb.OrientThreadedTransactionalGraphWrapper;
import com.gentics.mesh.graphdb.ThreadedTransactionalGraphWrapper;
import com.syncleus.ferma.DelegatingFramedThreadedTransactionalGraph;
import com.syncleus.ferma.DelegatingFramedTransactionalGraph;
import com.syncleus.ferma.FramedThreadedTransactionalGraph;
import com.syncleus.ferma.VertexFrame;
import com.tinkerpop.blueprints.impls.orient.OrientGraph;
import com.tinkerpop.blueprints.impls.orient.OrientGraphFactory;
import com.tinkerpop.blueprints.impls.orient.OrientTransactionalGraph;

public class OrientDBFermaMultithreadingTest extends AbstractOrientDBTest {

	OrientTransactionalGraph memoryGraph = new OrientGraph("memory:tinkerpop");
	OrientGraphFactory factory = new OrientGraphFactory("memory:tinkerpop");//.setupPool(5, 100);

	FramedThreadedTransactionalGraph fg;

	@Before
	public void setup() {
		ThreadedTransactionalGraphWrapper wrapper = new OrientThreadedTransactionalGraphWrapper(factory);
		fg = new DelegatingFramedThreadedTransactionalGraph<>(wrapper, true, false);
	}

	@Test
	public void testMultithreading() {
		Person p = addPersonWithFriends(fg, "SomePerson");
		p.setName("joe");
		fg.commit();
		runAndWait(() -> {
			try (BlueprintTransaction tx = new BlueprintTransaction(fg)) {
				manipulatePerson(p);
			}
		});
	}

	@Test
	public void testOrientThreadedTransactionalGraphWrapper() {

		// Test creation of user in current thread
		Person p = addPersonWithFriends(fg, "Person2");
		manipulatePerson(p);
		fg.commit();

		AtomicReference<Person> reference = new AtomicReference<>();
		runAndWait(() -> {
			try (BlueprintTransaction tx = new BlueprintTransaction(fg)) {
				manipulatePerson(p);
			}
			try (BlueprintTransaction tx = new BlueprintTransaction(fg)) {
				Person p2 = addPersonWithFriends(tx.getGraph(), "Person3");
				tx.success();
				reference.set(p2);
			}
			runAndWait(() -> {
				try (BlueprintTransaction tx = new BlueprintTransaction(fg)) {
					manipulatePerson(p);
				}
			});
		});

		for (VertexFrame vertex : fg.v().toList()) {
			System.out.println(vertex.toString());
		}

		try (BlueprintTransaction tx = new BlueprintTransaction(fg)) {
			manipulatePerson(reference.get());
		}
	}

}
