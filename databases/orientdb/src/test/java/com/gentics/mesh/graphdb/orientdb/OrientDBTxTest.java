package com.gentics.mesh.graphdb.orientdb;

import static com.gentics.mesh.graphdb.orientdb.ThreadUtils.run;
import static org.junit.Assert.assertEquals;

import java.util.concurrent.BrokenBarrierException;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

import com.gentics.madl.tx.Tx;
import com.gentics.mesh.graphdb.orientdb.graph.Person;
import com.gentics.mesh.graphdb.spi.Database;
import com.orientechnologies.orient.core.exception.OConcurrentModificationException;

public class OrientDBTxTest extends AbstractOrientDBTest {

	private Database db = mockDatabase();
	private Person p;

	@Before
	public void setup() throws Exception {
		db.init(null, null);
	}

	//	@Test
	//	public void testAsyncTrxRetryHandling() throws Exception {
	//
	//		AtomicInteger e = new AtomicInteger(0);
	//		String result = db.asyncTrx(() -> {
	//			e.incrementAndGet();
	//			if (e.get() == 1) {
	//				String msg = "Cannot UPDATE the record #13:8 because the version is not the latest. Probably you are updating an old record or it has been modified by another user (db=v7 your=v6)";
	//				// "test #9:1 blub adsd"
	//				throw new OConcurrentModificationException(msg);
	//			} else {
	//				return "OK";
	//			}
	//		}).toBlocking().single();
	//
	//		assertEquals(2, e.get());
	//		assertEquals("OK", result);
	//
	//	}

	@Test
	@Ignore
	public void testAsyncTxRetryHandling2() throws Exception {
		// Test creation of user in current thread
		long nFriendsBefore;
		try (Tx tx = db.tx()) {
			p = addPersonWithFriends(tx.getGraph(), "Person2");
			manipulatePerson(tx.getGraph(), p);
			tx.success();
			nFriendsBefore = p.getFriends().count();
		}

		CyclicBarrier b = new CyclicBarrier(3);
		AtomicInteger i = new AtomicInteger(0);

		run(() -> {
			db.tx((tx) -> {
				i.incrementAndGet();

				System.out.println("Tx1");
				addFriend(tx.getGraph(), p);
				if (i.get() <= 2) {
					b.await();
				}
				return null;
			});
		});

		run(() -> {
			db.tx((tx) -> {
				i.incrementAndGet();

				System.out.println("Tx2");
				addFriend(tx.getGraph(), p);
				if (i.get() <= 2) {
					b.await();
				}
				return null;
			});
		});

		b.await();
		Thread.sleep(1000);
		System.out.println("Asserting");
		try (Tx tx = db.tx()) {
			p = tx.getGraph().getFramedVertexExplicit(Person.class, p.getId());
			long nFriendsAfter = p.getFriends().count();
			assertEquals(nFriendsBefore + 2, nFriendsAfter);
		}

	}

	@Test
	@Ignore
	public void testTxConflictHandling() throws InterruptedException, BrokenBarrierException, TimeoutException {
		// Test creation of user in current thread
		long nFriendsBefore;
		try (Tx tx = db.tx()) {
			p = addPersonWithFriends(tx.getGraph(), "Person2");
			manipulatePerson(tx.getGraph(), p);
			tx.success();
			nFriendsBefore = p.getFriends().count();
		}

		CyclicBarrier b = new CyclicBarrier(3);

		addFriendToPerson(p, b);
		addFriendToPerson(p, b);

		b.await();
		Thread.sleep(1000);
		try (Tx tx = db.tx()) {
			p = tx.getGraph().getFramedVertexExplicit(Person.class, p.getId());
			long nFriendsAfter = p.getFriends().count();
			assertEquals(nFriendsBefore + 2, nFriendsAfter);
		}

	}

	private void addFriendToPerson(Person p, CyclicBarrier b) {
		run(() -> {
			for (int retry = 0; retry < 10; retry++) {
				System.out.println("Try: " + retry);
				//				try {
				try (Tx tx = db.tx()) {
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
