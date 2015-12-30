package com.gentics.mesh.graphdb.orientdb;

import static com.gentics.mesh.graphdb.orientdb.ThreadUtils.run;
import static org.junit.Assert.assertEquals;

import java.util.concurrent.BrokenBarrierException;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.Before;
import org.junit.Test;

import com.gentics.mesh.graphdb.OrientDBDatabase;
import com.gentics.mesh.graphdb.Trx;
import com.gentics.mesh.graphdb.orientdb.graph.Person;
import com.gentics.mesh.graphdb.spi.Database;
import com.orientechnologies.orient.core.exception.OConcurrentModificationException;

import io.vertx.core.Vertx;

public class OrientDBTrxTest extends AbstractOrientDBTest {

	private Database db = new OrientDBDatabase();
	private Person p;

	@Before
	public void setup() throws Exception {
		db.init(null, Vertx.vertx());
	}

	@Test
	public void testAsyncTrxRetryHandling() throws Exception {

		AtomicInteger e = new AtomicInteger(0);
		String result = db.asyncTrx(() -> {
			e.incrementAndGet();
			if (e.get() == 1) {
				String msg = "Cannot UPDATE the record #13:8 because the version is not the latest. Probably you are updating an old record or it has been modified by another user (db=v7 your=v6)";
				// "test #9:1 blub adsd"
				throw new OConcurrentModificationException(msg);
			} else {
				return "OK";
			}
		}).toBlocking().single();

		assertEquals(2, e.get());
		assertEquals("OK", result);

	}

	@Test
	public void testAsyncTrxRetryHandling2() throws Exception {
		// Test creation of user in current thread
		int nFriendsBefore;
		try (Trx tx = db.trx()) {
			p = addPersonWithFriends(tx.getGraph(), "Person2");
			manipulatePerson(tx.getGraph(), p);
			tx.success();
			nFriendsBefore = p.getFriends().size();
		}

		CyclicBarrier b = new CyclicBarrier(3);
		AtomicInteger i = new AtomicInteger(0);

		run(() -> {
			db.trx(() -> {
				i.incrementAndGet();

				System.out.println("Trx1");
				addFriend(Database.getThreadLocalGraph(), p);
				if (i.get() <= 2) {
					b.await();
				}
				return null;
			});
		});

		run(() -> {
			db.trx(() -> {
				i.incrementAndGet();

				System.out.println("Trx2");
				addFriend(Database.getThreadLocalGraph(), p);
				if (i.get() <= 2) {
					b.await();
				}
				return null;
			});
		});

		b.await();
		Thread.sleep(1000);
		System.out.println("Asserting");
		try (Trx tx = db.trx()) {
			p = tx.getGraph().getFramedVertexExplicit(Person.class, p.getId());
			int nFriendsAfter = p.getFriends().size();
			assertEquals(nFriendsBefore + 2, nFriendsAfter);
		}

	}

	@Test
	public void testTrxConflictHandling() throws InterruptedException, BrokenBarrierException, TimeoutException {
		// Test creation of user in current thread
		int nFriendsBefore;
		try (Trx tx = db.trx()) {
			p = addPersonWithFriends(tx.getGraph(), "Person2");
			manipulatePerson(tx.getGraph(), p);
			tx.success();
			nFriendsBefore = p.getFriends().size();
		}

		CyclicBarrier b = new CyclicBarrier(3);

		addFriendToPerson(p, b);
		addFriendToPerson(p, b);

		b.await();
		Thread.sleep(1000);
		try (Trx tx = db.trx()) {
			p = tx.getGraph().getFramedVertexExplicit(Person.class, p.getId());
			int nFriendsAfter = p.getFriends().size();
			assertEquals(nFriendsBefore + 2, nFriendsAfter);
		}

	}

	private void addFriendToPerson(Person p, CyclicBarrier b) {
		run(() -> {
			for (int retry = 0; retry < 10; retry++) {
				System.out.println("Try: " + retry);
				boolean doRetry = false;
				//				try {
				try (Trx tx = db.trx()) {
					addFriend(tx.getGraph(), p);
					tx.success();
					if (retry == 0) {
						try {
							b.await();
						} catch (Exception e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						}
					}
					break;
				} catch (OConcurrentModificationException e) {
					//throw e;
					//break;
				}
				//				} catch (OConcurrentModificationException e) {
				//					System.out.println("Error " + OConcurrentModificationException.class.getName());
				//					doRetry = true;
				//				}
				//				if (!doRetry) {
				//					break;
				//				}
				System.out.println("Retry");
			}
		});
	}
}
