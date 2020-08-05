package com.gentics.mesh.graphdb.orientdb;

import static com.gentics.mesh.graphdb.orientdb.ThreadUtils.runAndWait;

import java.util.concurrent.BrokenBarrierException;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.Before;
import org.junit.Test;

import com.gentics.mesh.core.db.Tx;
import com.gentics.mesh.etc.config.MeshOptions;
import com.gentics.mesh.graphdb.orientdb.graph.Person;
import com.gentics.mesh.graphdb.spi.Database;
import com.syncleus.ferma.VertexFrame;

public class OrientDBFermaMultithreadingTest extends AbstractOrientDBTest {

	private Database db = mockDatabase();

	@Before
	public void setup() throws Exception {
		MeshOptions options = new MeshOptions();
		options.setNodeName("dummy");
		db.init(options, null,"com.gentics.mesh.graphdb.orientdb.graph");
		db.setupConnectionPool();
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
		try (Tx tx = db.tx()) {
			p = addPersonWithFriends(tx.getGraph(), "SomePerson");
			p.setName("joe");
			tx.success();
		}
		runAndWait(() -> {
			try (Tx tx = db.tx()) {
				manipulatePerson(tx.getGraph(), p);
			}
		});
	}

	@Test
	public void testOrientThreadedTransactionalGraphWrapper() {

		// Test creation of user in current thread
		try (Tx tx = db.tx()) {
			Person p = addPersonWithFriends(tx.getGraph(), "Person2");
			manipulatePerson(tx.getGraph(), p);
			tx.success();
		}

		AtomicReference<Person> reference = new AtomicReference<>();
		runAndWait(() -> {
			try (Tx tx = db.tx()) {
				manipulatePerson(tx.getGraph(), p);
			}
			try (Tx tx = db.tx()) {
				Person p2 = addPersonWithFriends(tx.getGraph(), "Person3");
				tx.success();
				reference.set(p2);
			}
			runAndWait(() -> {
				try (Tx tx = db.tx()) {
					manipulatePerson(tx.getGraph(), p);
				}
			});
		});

		try (Tx tx = db.tx()) {
			for (VertexFrame vertex : tx.getGraph().v().frameExplicit(VertexFrame.class)) {
				System.out.println(vertex.toString());
			}
		}
		// try (Tx tx = db.tx()) {
		// manipulatePerson(tx.getGraph(), reference.get());
		// }
	}

}
