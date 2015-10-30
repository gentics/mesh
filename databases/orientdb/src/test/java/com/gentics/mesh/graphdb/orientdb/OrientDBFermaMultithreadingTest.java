package com.gentics.mesh.graphdb.orientdb;

import static com.gentics.mesh.graphdb.orientdb.ThreadUtils.runAndWait;

import java.util.concurrent.BrokenBarrierException;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.Before;
import org.junit.Test;

import com.gentics.mesh.graphdb.OrientDBDatabase;
import com.gentics.mesh.graphdb.Trx;
import com.gentics.mesh.graphdb.orientdb.graph.Person;
import com.gentics.mesh.graphdb.spi.Database;
import com.syncleus.ferma.VertexFrame;

import io.vertx.core.Vertx;

public class OrientDBFermaMultithreadingTest extends AbstractOrientDBTest {

	private Database db = new OrientDBDatabase();

	@Before
	public void setup() throws Exception {
		db.init(null, Vertx.vertx());
	}

	Person p;

	@Test
	public void testCyclicBarrier() throws InterruptedException, BrokenBarrierException {
		int nThreads = 3;
		CyclicBarrier barrier = new CyclicBarrier(nThreads);
		for (int i = 0; i < nThreads; i++) {
			Thread.sleep(1000);
			TestThread t = new TestThread(i, barrier);
			t.start();
		}

		Thread.sleep(4000);

	}

	@Test
	public void testMultithreading() {
		try (Trx tx = db.trx()) {
			p = addPersonWithFriends(tx.getGraph(), "SomePerson");
			p.setName("joe");
			tx.success();
		}
		runAndWait(() -> {
			try (Trx tx = db.trx()) {
				manipulatePerson(tx.getGraph(), p);
			}
		});
	}

	@Test
	public void testOrientThreadedTransactionalGraphWrapper() {

		// Test creation of user in current thread
		try (Trx tx = db.trx()) {
			Person p = addPersonWithFriends(tx.getGraph(), "Person2");
			manipulatePerson(tx.getGraph(), p);
			tx.success();
		}

		AtomicReference<Person> reference = new AtomicReference<>();
		runAndWait(() -> {
			try (Trx tx = db.trx()) {
				manipulatePerson(tx.getGraph(), p);
			}
			try (Trx tx = db.trx()) {
				Person p2 = addPersonWithFriends(tx.getGraph(), "Person3");
				tx.success();
				reference.set(p2);
			}
			runAndWait(() -> {
				try (Trx tx = db.trx()) {
					manipulatePerson(tx.getGraph(), p);
				}
			});
		});

		try (Trx tx = db.trx()) {
			for (VertexFrame vertex : tx.getGraph().v().toList()) {
				System.out.println(vertex.toString());
			}
		}
		// try (Trx tx = db.trx()) {
		// manipulatePerson(tx.getGraph(), reference.get());
		// }
	}

}
