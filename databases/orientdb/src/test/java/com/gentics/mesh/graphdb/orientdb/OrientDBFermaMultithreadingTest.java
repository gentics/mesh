package com.gentics.mesh.graphdb.orientdb;

import java.util.concurrent.atomic.AtomicReference;

import org.junit.Before;
import org.junit.Test;

import com.gentics.mesh.graphdb.BlueprintTransaction;
import com.gentics.mesh.graphdb.OrientThreadedTransactionalGraphWrapper;
import com.gentics.mesh.graphdb.ThreadedTransactionalGraphWrapper;
import com.syncleus.ferma.DelegatingFramedThreadedTransactionalGraph;
import com.syncleus.ferma.FramedThreadedTransactionalGraph;
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
		Person p = addPersonWithFriends(fg);
		p.setName("joe");
		runAndWait(() -> {
			try (BlueprintTransaction tx = new BlueprintTransaction(fg)) {
				manipulatePerson(p);
			}
		});
	}

	@Test
	public void testOrientThreadedTransactionalGraphWrapper() {

		// Test creation of user in current thread
		Person p = addPersonWithFriends(fg);
		manipulatePerson(p);

		AtomicReference<Person> reference = new AtomicReference<>();
		runAndWait(() -> {
			try (BlueprintTransaction tx = new BlueprintTransaction(fg)) {
				Person p2 = addPersonWithFriends(fg);
				reference.set(p2);
				tx.success();
			}
			try (BlueprintTransaction tx = new BlueprintTransaction(fg)) {
				manipulatePerson(p);
			}
			runAndWait(() -> {
				try (BlueprintTransaction tx = new BlueprintTransaction(fg)) {
					manipulatePerson(p);
				}
			});
		});
		try (BlueprintTransaction tx = new BlueprintTransaction(fg)) {
			manipulatePerson(reference.get());
		}
	}

}
