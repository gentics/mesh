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
		setupData();
	}

	Person p;

	private void setupData() {
		try (BlueprintTransaction tx = new BlueprintTransaction(fg)) {
			String name = "SomeName";
			p = addPersonWithFriends(fg, name);
			System.out.println(p.getGraph().getClass().getName());
			tx.success();
			//fg.commit();
		}
	}

	@Test
	public void testMultithreading() {

//		fg.commit();
		runAndWait(() -> {
			try (BlueprintTransaction tx = new BlueprintTransaction(fg)) {
				manipulatePerson(p);
				String name = "newName";
				p.setName(name);

				Person reloaded = tx.getGraph().v().has(Person.class).has("name", name).nextOrDefaultExplicit(Person.class, null);
				System.out.println(reloaded.getName());
				assertNotNull(reloaded);
				manipulatePerson(reloaded);
			}
		});
	}

}
