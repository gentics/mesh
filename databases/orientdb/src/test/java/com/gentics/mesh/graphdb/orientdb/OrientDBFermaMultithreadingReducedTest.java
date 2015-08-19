package com.gentics.mesh.graphdb.orientdb;

import static org.junit.Assert.assertNotNull;

import org.junit.Before;
import org.junit.Test;

import com.gentics.mesh.graphdb.Trx;
import com.gentics.mesh.graphdb.OrientDBDatabase;
import com.gentics.mesh.graphdb.spi.Database;

public class OrientDBFermaMultithreadingReducedTest extends AbstractOrientDBTest {

	private Database database;
	private Person p;

	@Before
	public void setup() {
		database = new OrientDBDatabase();
		database.init(null);
		setupData();
	}

	private void setupData() {
		//TransactionalGraph graph = fg.newTransaction();
		//FramedTransactionalGraph fg2 = new DelegatingFramedTransactionalGraph<>(graph, true, false);
		try (Trx tx = new Trx(database)) {
			String name = "SomeName";
			p = addPersonWithFriends(tx.getGraph(), name);
			tx.getGraph().commit();
			tx.success();
			runAndWait(() -> {
				try (Trx tx2 = new Trx(database)) {
					readPerson(p);
					manipulatePerson(tx2.getGraph(), p);
				}
			});
		}

		runAndWait(() -> {
			try (Trx tx2 = new Trx(database)) {
				readPerson(p);
				manipulatePerson(tx2.getGraph(), p);
			}
		});

	}

	@Test
	public void testMultithreading() {

		//		fg.commit();
		runAndWait(() -> {
			Person reloaded;
			try (Trx tx = new Trx(database)) {
				manipulatePerson(tx.getGraph(), p);
				String name = "newName";
				p.setName(name);
				reloaded = tx.getGraph().v().has(Person.class).has("name", name).nextOrDefaultExplicit(Person.class, null);
				System.out.println(reloaded.getName());
				assertNotNull(reloaded);
				manipulatePerson(tx.getGraph(), reloaded);
				tx.success();
			}
			runAndWait(() -> {
				try (Trx tx2 = new Trx(database)) {
					readPerson(reloaded);
				}
			});
		});
	}

	private void readPerson(Person person) {
		person.getName();
		for (Person p : person.getFriends()) {
			p.getName();
			for (Person p2 : person.getFriends()) {
				p2.getName();
				for (Person p3 : p2.getFriends()) {
					p3.getName();
				}
			}
		}
	}

}
