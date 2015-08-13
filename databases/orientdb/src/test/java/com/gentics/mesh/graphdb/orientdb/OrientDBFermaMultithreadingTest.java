package com.gentics.mesh.graphdb.orientdb;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.util.concurrent.atomic.AtomicReference;

import org.junit.Before;
import org.junit.Test;

import com.gentics.mesh.graphdb.BlueprintTransaction;
import com.gentics.mesh.graphdb.OrientThreadedTransactionalGraphWrapper;
import com.gentics.mesh.graphdb.ThreadedTransactionalGraphWrapper;
import com.syncleus.ferma.DelegatingFramedThreadedTransactionalGraph;
import com.syncleus.ferma.DelegatingFramedTransactionalGraph;
import com.syncleus.ferma.FramedThreadedTransactionalGraph;
import com.syncleus.ferma.WrapperFramedTransactionalGraph;
import com.tinkerpop.blueprints.impls.orient.OrientGraph;
import com.tinkerpop.blueprints.impls.orient.OrientGraphFactory;
import com.tinkerpop.blueprints.impls.orient.OrientTransactionalGraph;

public class OrientDBFermaMultithreadingTest {

	OrientTransactionalGraph memoryGraph = new OrientGraph("memory:tinkerpop");
	OrientGraphFactory factory = new OrientGraphFactory("memory:tinkerpop");//.setupPool(5, 100);

	FramedThreadedTransactionalGraph fg;
	//WrapperFramedTransactionalGraph<OrientTransactionalGraph> fg;

	@Before
	public void setup() {
		ThreadedTransactionalGraphWrapper wrapper = new OrientThreadedTransactionalGraphWrapper(factory);
		fg = new DelegatingFramedThreadedTransactionalGraph<>(wrapper, true, false);
	}

	private Person addPersonWithFriends() {
		Person p = fg.addFramedVertex(Person.class);
		p.setName("SomePerson");

		for (int i = 0; i < 10; i++) {
			Person friend = fg.addFramedVertex(Person.class);
			p.setName("Friend " + i);
			p.addFriend(friend);
		}
		return p;
	}

	@Test
	public void testMultithreading() {

		WrapperFramedTransactionalGraph<OrientTransactionalGraph> fg = new DelegatingFramedTransactionalGraph<>(memoryGraph, true, false);

		Person p = addPersonWithFriends();

		p.setName("joe");
		assertEquals("joe", p.getName());
		Person p1 = fg.v().has(Person.class).nextOrDefaultExplicit(Person.class, null);
		assertNotNull(p1);
		fg.commit();

		p1.setName("dgasgds");
		AtomicReference<Person> reference = new AtomicReference<>();
		runAndWait(() -> {
			p.setName("Blablkabla");
			Person p2 = fg.addFramedVertex(Person.class);
			p2.setName("Huibuh");
			reference.set(p2);
			fg.commit();
		});
		fg.rollback();
		reference.get().setName("asegasdg");

		runAndWait(() -> {
			Person foundPerson = fg.v().has(Person.class).has("name", "joe").nextOrDefaultExplicit(Person.class, null);
			assertNotNull(foundPerson);
			assertEquals("joe", foundPerson.getName());
			foundPerson.setName("Marko");
			fg.rollback();
		});
		assertEquals("joe", p1.getName());

	}

	private void manipulatePerson(Person p) {
		p.setName("Changed " + System.currentTimeMillis());
		for (Person friend : p.getFriends()) {
			friend.setName("Changed Name " + System.currentTimeMillis());
		}
	}

	@Test
	public void testOrientThreadedTransactionalGraphWrapper() {

		// Test creation of user in current thread
		Person p = addPersonWithFriends();
		manipulatePerson(p);

		AtomicReference<Person> reference = new AtomicReference<>();
		runAndWait(() -> {
			manipulatePerson(p);
			Person p2 = addPersonWithFriends();
			reference.set(p2);
			runAndWait(() -> {
				manipulatePerson(p);
				manipulatePerson(p2);
			});
			//			fg.commit();
		});
		try (BlueprintTransaction tx = new BlueprintTransaction(fg)) {
			manipulatePerson(reference.get());
		}
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
