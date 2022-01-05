package com.gentics.mesh.graphdb;

import static com.gentics.mesh.test.TestSize.FULL;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.BrokenBarrierException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.Ignore;
import org.junit.Test;

import com.gentics.mesh.core.data.dao.TagDao;
import com.gentics.mesh.core.data.dao.UserDao;
import com.gentics.mesh.core.data.node.HibNode;
import com.gentics.mesh.core.data.project.HibProject;
import com.gentics.mesh.core.data.tag.HibTag;
import com.gentics.mesh.core.data.tagfamily.HibTagFamily;
import com.gentics.mesh.core.data.user.HibUser;
import com.gentics.mesh.core.db.CommonTx;
import com.gentics.mesh.core.db.Tx;
import com.gentics.mesh.test.MeshTestSetting;
import com.gentics.mesh.test.context.AbstractMeshTest;
import com.gentics.mesh.test.util.TestUtils;

import io.reactivex.Single;

@MeshTestSetting(testSize = FULL, startServer = true)
public class TxTest extends AbstractMeshTest {

	@Test
	public void testTransaction() throws InterruptedException {
		AtomicInteger i = new AtomicInteger(0);

		int e = i.incrementAndGet();
		try (Tx tx = tx()) {
			UserDao userDao= tx.userDao();
			assertNotNull(userDao.create("testuser" + e, user()));
			assertNotNull(boot().userDao().findByUsername("testuser" + e));
			tx.success();
		}
		try (Tx tx = tx()) {
			assertNotNull(boot().userDao().findByUsername("testuser" + e));
		}
		int u = i.incrementAndGet();
		Runnable task = () -> {
			try (Tx tx = tx()) {
				UserDao userDao= tx.userDao();
				assertNotNull(userDao.create("testuser" + u, user()));
				assertNotNull(userDao.findByUsername("testuser" + u));
				tx.failure();
			}
			assertNull(boot().userDao().findByUsername("testuser" + u));

		};
		Thread t = new Thread(task);
		t.start();
		t.join();
		try (Tx tx = tx()) {
			assertNull(boot().userDao().findByUsername("testuser" + u));
			System.out.println("RUN: " + i.get());
		}

	}

	@Test
	public void testMultiThreadedModifications() throws InterruptedException {
		HibUser user = db().tx(() -> user());

		Runnable task2 = () -> {
			try (Tx tx = tx()) {
				user.setUsername("test2");
				assertNotNull(boot().userDao().findByUsername("test2"));
				tx.success();
			}
			assertNotNull(boot().userDao().findByUsername("test2"));

			Runnable task = () -> {
				try (Tx tx = tx()) {
					user.setUsername("test3");
					assertNotNull(boot().userDao().findByUsername("test3"));
					tx.failure();
				}
				assertNotNull(boot().userDao().findByUsername("test2"));
				assertNull(boot().userDao().findByUsername("test3"));

			};
			Thread t = new Thread(task);
			t.start();
			try {
				t.join();
			} catch (Exception e) {
				e.printStackTrace();
			}
		};
		Thread t2 = new Thread(task2);
		t2.start();
		t2.join();
		try (Tx tx = tx()) {
			assertNull(boot().userDao().findByUsername("test3"));
			assertNotNull("The user with username test2 could not be found.", boot().userDao().findByUsername("test2"));
		}

	}

	@Test(expected = RuntimeException.class)
	public void testAsyncNoTrxWithError() throws Throwable {
		CompletableFuture<Throwable> cf = new CompletableFuture<>();
		db().asyncTx(() -> {
			throw new RuntimeException("error");
		}).blockingGet();
		assertEquals("error", cf.get().getMessage());
		throw cf.get();
	}

	@Test
	public void testAsyncNoTrxNestedAsync() throws InterruptedException, ExecutionException {
		String result = db().asyncTx(() -> {
			TestUtils.run(() -> {
				TestUtils.sleep(1000);
			});
			return Single.just("OK");
		}).blockingGet();
		assertEquals("OK", result);
	}

	@Test
	public void testAsyncNoTrxSuccess() throws Throwable {
		String result = db().asyncTx(() -> {
			return Single.just("OK");
		}).blockingGet();
		assertEquals("OK", result);
	}

	@Test
	@Ignore
	public void testUpdateMultithreaded() throws InterruptedException, BrokenBarrierException, TimeoutException {

		final int nThreads = 10;
		final int nRuns = 20;
		final int maxRetry = 20;

		for (int r = 0; r < nRuns; r++) {
			final int currentRun = r;
			System.out.println("\n\n\n\n");
			// TraversalHelper.printDebugVertices();
			CyclicBarrier barrierA = new CyclicBarrier(nThreads);
			CyclicBarrier barrierB = new CyclicBarrier(nThreads);
			HibNode node = content();
			HibTagFamily tagFamily = tagFamily("colors");
			List<Thread> threads = new ArrayList<>();
			HibProject project = project();
			HibUser user = user();

			for (int i = 0; i < nThreads; i++) {
				final int threadNo = i;
				System.out.println("Thread [" + threadNo + "] Starting");
				Thread t = TestUtils.run(() -> {

					for (int retry = 0; retry < maxRetry; retry++) {
						try {
							try (Tx tx = tx()) {
								CommonTx ctx = tx.unwrap();
								TagDao tagDao = tx.tagDao();
								if (retry == 0) {
									try {
										System.out.println("Thread [" + threadNo + "] Waiting..");
										barrierA.await(10, TimeUnit.SECONDS);
										System.out.println("Thread [" + threadNo + "] Waited");
									} catch (Exception e) {
										System.out.println("Thread [" + threadNo + "] Error handling barrier timeout? - retry: " + retry);
										// e.printStackTrace();
									}
								}
								// Load used elements
								HibTagFamily reloadedTagFamily = ctx.load(tagFamily.getId(), ctx.tagFamilyDao().getPersistenceClass(project()));
								HibNode reloadedNode = ctx.load(node.getId(), ctx.nodeDao().getPersistenceClass(project));
								HibUser reloadedUser = ctx.load(user.getId(), ctx.userDao().getPersistenceClass());
								HibProject reloadedProject = ctx.load(project.getId(), ctx.projectDao().getPersistenceClass());

								HibTag tag = tagDao.create(reloadedTagFamily, "bogus_" + threadNo + "_" + currentRun, project(), reloadedUser);
								// Reload the node
								tagDao.addTag(reloadedNode, tag, reloadedProject.getLatestBranch());
								tx.success();
								if (retry == 0) {
									try {
										System.out.println("Thread [" + threadNo + "] Waiting..");
										barrierB.await(10, TimeUnit.SECONDS);
										System.out.println("Thread [" + threadNo + "] Waited");
									} catch (Exception e) {
										System.out.println("Thread [" + threadNo + "] Error handling barrier timeout? - retry: " + retry);
										// e.printStackTrace();
									}
								}
							}
							System.out.println("Thread [" + threadNo + "] Successful updated element - retry: " + retry);
							break;
						} catch (Exception e) {

							// trx.rollback();
							System.out.println("Thread [" + threadNo + "] Got exception {" + e.getClass().getName() + "}  - retry: " + retry);
							e.printStackTrace();
						}
					}
				});
				threads.add(t);
			}
			System.out.println("Waiting on lock");
			// barrier.blockingAwait()(2, TimeUnit.SECONDS);
			for (Thread currentThread : threads) {
				currentThread.join();
			}
			// Thread.sleep(1000);
			try (Tx tx = tx()) {
				CommonTx ctx = tx.unwrap();
				TagDao tagDao = tx.tagDao();

				int expect = nThreads * (r + 1);
				HibNode reloadedNode = ctx.load(node.getId(), ctx.nodeDao().getPersistenceClass(project));
				// node.reload();
				assertEquals("Expected {" + expect + "} tags since this is run {" + r + "}.", expect,
						tagDao.getTags(reloadedNode, project().getLatestBranch()).count());
			}
		}
	}
}
