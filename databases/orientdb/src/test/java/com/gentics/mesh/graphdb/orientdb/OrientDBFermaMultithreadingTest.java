package com.gentics.mesh.graphdb.orientdb;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import org.junit.Before;
import org.junit.Test;

import com.syncleus.ferma.DelegatingFramedTransactionalGraph;
import com.syncleus.ferma.WrapperFramedTransactionalGraph;
import com.tinkerpop.blueprints.impls.orient.OrientGraph;
import com.tinkerpop.blueprints.impls.orient.OrientTransactionalGraph;

public class OrientDBFermaMultithreadingTest {

	OrientTransactionalGraph memoryGraph = new OrientGraph("memory:tinkerpop");

	@Before
	public void cleanup() {

	}

	@Test
	public void testMultithreading() {

		WrapperFramedTransactionalGraph<OrientTransactionalGraph> fg = new DelegatingFramedTransactionalGraph<>(memoryGraph, true, false);
		Person p = fg.addFramedVertex(Person.class);
		p.setName("joe");
		assertEquals("joe", p.getName());
		Person p1 = fg.v().has(Person.class).nextOrDefaultExplicit(Person.class, null);
		assertNotNull(p1);
		fg.commit();

		p1.setName("dgasgds");
		runAndWait(() -> {
			Person p2 = fg.addFramedVertex(Person.class);
			p2.setName("Huibuh");
			fg.commit();
		});
		fg.rollback();

		runAndWait(() -> {
			Person foundPerson = fg.v().has(Person.class).has("name", "joe").nextOrDefaultExplicit(Person.class, null);
			assertNotNull(foundPerson);
			assertEquals("joe", foundPerson.getName());
			foundPerson.setName("Marko");
			fg.rollback();
		});
		assertEquals("joe", p1.getName());

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
