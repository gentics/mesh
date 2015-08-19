package com.gentics.mesh.graphdb.orientdb;

import java.util.concurrent.atomic.AtomicReference;

import org.junit.Before;
import org.junit.Test;

import com.gentics.mesh.graphdb.Trx;
import com.gentics.mesh.graphdb.OrientDBDatabase;
import com.gentics.mesh.graphdb.spi.Database;
import com.syncleus.ferma.VertexFrame;

public class OrientDBFermaMultithreadingTest extends AbstractOrientDBTest {

	private Database database = new OrientDBDatabase();

	@Before
	public void setup() {
		database.init(null);
	}

	Person p;

	@Test
	public void testMultithreading() {
		try (Trx tx = new Trx(database)) {
			p = addPersonWithFriends(tx.getGraph(), "SomePerson");
			p.setName("joe");
			tx.success();
		}
		runAndWait(() -> {
			try (Trx tx = new Trx(database)) {
				manipulatePerson(tx.getGraph(), p);
			}
		});
	}

	@Test
	public void testOrientThreadedTransactionalGraphWrapper() {

		// Test creation of user in current thread
		try (Trx tx = new Trx(database)) {
			Person p = addPersonWithFriends(tx.getGraph(), "Person2");
			manipulatePerson(tx.getGraph(), p);
			tx.success();
		}

		AtomicReference<Person> reference = new AtomicReference<>();
		runAndWait(() -> {
			try (Trx tx = new Trx(database)) {
				manipulatePerson(tx.getGraph(), p);
			}
			try (Trx tx = new Trx(database)) {
				Person p2 = addPersonWithFriends(tx.getGraph(), "Person3");
				tx.success();
				reference.set(p2);
			}
			runAndWait(() -> {
				try (Trx tx = new Trx(database)) {
					manipulatePerson(tx.getGraph(), p);
				}
			});
		});

		try (Trx tx = new Trx(database)) {
			for (VertexFrame vertex : tx.getGraph().v().toList()) {
				System.out.println(vertex.toString());
			}
		}
		try (Trx tx = new Trx(database)) {
			manipulatePerson(tx.getGraph(), reference.get());
		}
	}

}
