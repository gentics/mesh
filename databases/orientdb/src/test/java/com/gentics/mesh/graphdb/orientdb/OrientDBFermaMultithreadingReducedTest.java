package com.gentics.mesh.graphdb.orientdb;

import static com.gentics.mesh.graphdb.orientdb.ThreadUtils.runAndWait;
import static org.junit.Assert.assertNotNull;

import org.junit.Before;
import org.junit.Test;

import com.gentics.mesh.etc.config.MeshOptions;
import com.gentics.mesh.graphdb.OrientDBDatabase;
import com.gentics.mesh.graphdb.orientdb.graph.Person;
import com.gentics.mesh.graphdb.spi.Database;
import com.syncleus.ferma.tx.Tx;

public class OrientDBFermaMultithreadingReducedTest extends AbstractOrientDBTest {

	private Database db;
	private Person p;

	@Before
	public void setup() throws Exception {
		db = mockDatabase();
		MeshOptions options = new MeshOptions();
		options.getStorageOptions().setDirectory(null);
		db.init(options, "com.gentics.mesh.graphdb.orientdb.graph");
		db.setupConnectionPool();
		setupData();
	}

	private void setupData() {
		try (Tx tx = db.tx()) {
			String name = "SomeName";
			p = addPersonWithFriends(tx.getGraph(), name);
			// tx.getGraph().commit();
			tx.success();
			runAndWait(() -> {
				try (Tx tx2 = db.tx()) {
					readPerson(p);
					manipulatePerson(tx2.getGraph(), p);
				}
			});
		}

		runAndWait(() -> {
			try (Tx tx2 = db.tx()) {
				readPerson(p);
				manipulatePerson(tx2.getGraph(), p);
			}
		});

	}

	@Test
	public void testMultithreading() {

		// fg.commit();
		runAndWait(() -> {
			Person reloaded;
			try (Tx tx = db.tx()) {
				manipulatePerson(tx.getGraph(), p);
				String name = "newName";
				p.setName(name);
				reloaded = tx.getGraph().v().has(Person.class).has("name", name).nextOrDefaultExplicit(Person.class,
						null);
				System.out.println(reloaded.getName());
				assertNotNull(reloaded);
				manipulatePerson(tx.getGraph(), reloaded);
				tx.success();
			}
			runAndWait(() -> {
				try (Tx tx2 = db.tx()) {
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
