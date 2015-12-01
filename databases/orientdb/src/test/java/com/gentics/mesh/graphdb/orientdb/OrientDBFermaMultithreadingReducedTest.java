package com.gentics.mesh.graphdb.orientdb;

import static org.junit.Assert.assertNotNull;

import org.junit.Before;
import org.junit.Test;

import static com.gentics.mesh.graphdb.orientdb.ThreadUtils.*;
import com.gentics.mesh.graphdb.OrientDBDatabase;
import com.gentics.mesh.graphdb.Trx;
import com.gentics.mesh.graphdb.orientdb.graph.Person;
import com.gentics.mesh.graphdb.spi.Database;

import io.vertx.core.Vertx;

public class OrientDBFermaMultithreadingReducedTest extends AbstractOrientDBTest {

	private Database db;
	private Person p;

	@Before
	public void setup() throws Exception {
		db = new OrientDBDatabase();
		db.init(null, Vertx.vertx());
		setupData();
	}

	private void setupData() {
		try (Trx tx = db.trx()) {
			String name = "SomeName";
			p = addPersonWithFriends(tx.getGraph(), name);
//			tx.getGraph().commit();
			tx.success();
			runAndWait(() -> {
				try (Trx tx2 = db.trx()) {
					readPerson(p);
					manipulatePerson(tx2.getGraph(), p);
				}
			});
		}

		runAndWait(() -> {
			try (Trx tx2 = db.trx()) {
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
			try (Trx tx = db.trx()) {
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
				try (Trx tx2 = db.trx()) {
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
