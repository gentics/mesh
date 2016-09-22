package com.gentics.mesh.graphdb;

import static com.gentics.mesh.core.verticle.handler.HandlerUtilities.operateNoTx;
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

import com.gentics.mesh.core.data.AbstractIsolatedBasicDBTest;
import com.gentics.mesh.core.data.Project;
import com.gentics.mesh.core.data.Tag;
import com.gentics.mesh.core.data.TagFamily;
import com.gentics.mesh.core.data.User;
import com.gentics.mesh.core.data.impl.ProjectImpl;
import com.gentics.mesh.core.data.impl.TagFamilyImpl;
import com.gentics.mesh.core.data.impl.UserImpl;
import com.gentics.mesh.core.data.node.Node;
import com.gentics.mesh.core.data.node.impl.NodeImpl;
import com.gentics.mesh.core.data.root.UserRoot;
import com.gentics.mesh.test.performance.TestUtils;

import rx.Single;

public class TrxTest extends AbstractIsolatedBasicDBTest {

	//	@Test
	//	public void testAsyncTestErrorHandling() throws Exception {
	//		CompletableFuture<AsyncResult<Object>> fut = new CompletableFuture<>();
	//		db.asyncTrx(() -> {
	//			throw new Exception("error");
	//		});
	//
	//		AsyncResult<Object> result = fut.get(5, TimeUnit.SECONDS);
	//		assertTrue(result.failed());
	//		assertNotNull(result.cause());
	//		assertEquals("blub", result.cause().getMessage());
	//	}

	@Test
	public void testReload() {
		try (Tx tx = db.tx()) {
			user().reload();
		}
	}

	//	@Test
	//	public void testAsyncTestSuccessHandling() throws Exception {
	//		String result = db.asyncTrx(() -> {
	//			return "test";
	//		}).toBlocking().first();
	//		assertEquals("test", result);
	//	}

	//	@Test
	//	public void testConcurrentUpdate() throws Exception {
	//		final int nThreads = 10;
	//		final int nRuns = 200;
	//
	//		try (Trx tx2 = db.trx()) {
	//			TagFamily tagFamily = tagFamily("colors");
	//			Node node = content();
	//			for (int r = 1; r <= nRuns; r++) {
	//				final int currentRun = r;
	//				CountDownLatch latch = new CountDownLatch(nThreads);
	//
	//				// Start two threads with a retry trx
	//				for (int i = 0; i < nThreads; i++) {
	//					final int threadNo = i;
	//					if (log.isTraceEnabled()) {
	//						log.trace("Thread [" + threadNo + "] Starting");
	//					}
	//					db.asyncTrx(() -> {
	//						Tag tag = tagFamily.create("bogus_" + threadNo + "_" + currentRun, project(), user());
	//						node.addTag(tag);
	//						return tag;
	//					}).subscribe(tag -> {
	//						assertEquals(TagImpl.class, tag.getClass());
	//						latch.countDown();
	//					});
	//				}
	//
	//				log.debug("Waiting on lock");
	//				failingLatch(latch);
	//
	//				try (Trx tx = db.trx()) {
	//					int expect = nThreads * r;
	//					assertEquals("Expected {" + expect + "} tags since this is the " + r + "th run.", expect, content().getTags().size());
	//				}
	//			}
	//		}
	//	}

	@Test
	public void testTransaction() throws InterruptedException {
		AtomicInteger i = new AtomicInteger(0);

		UserRoot root;
		try (Tx tx = db.tx()) {
			root = meshRoot().getUserRoot();
		}
		int e = i.incrementAndGet();
		try (Tx tx = db.tx()) {
			assertNotNull(root.create("testuser" + e, user()));
			assertNotNull(boot.userRoot().findByUsername("testuser" + e));
			tx.success();
		}
		try (Tx tx = db.tx()) {
			assertNotNull(boot.userRoot().findByUsername("testuser" + e));
		}
		int u = i.incrementAndGet();
		Runnable task = () -> {
			try (Tx tx = db.tx()) {
				assertNotNull(root.create("testuser" + u, user()));
				assertNotNull(boot.userRoot().findByUsername("testuser" + u));
				tx.failure();
			}
			assertNull(boot.userRoot().findByUsername("testuser" + u));

		};
		Thread t = new Thread(task);
		t.start();
		t.join();
		try (Tx tx = db.tx()) {
			assertNull(boot.userRoot().findByUsername("testuser" + u));
			System.out.println("RUN: " + i.get());
		}

	}

	@Test
	public void testMultiThreadedModifications() throws InterruptedException {
		User user = db.noTx(() -> user());

		Runnable task2 = () -> {
			try (Tx tx = db.tx()) {
				user.setUsername("test2");
				assertNotNull(boot.userRoot().findByUsername("test2"));
				tx.success();
			}
			assertNotNull(boot.userRoot().findByUsername("test2"));

			Runnable task = () -> {
				try (Tx tx = db.tx()) {
					user.setUsername("test3");
					assertNotNull(boot.userRoot().findByUsername("test3"));
					tx.failure();
				}
				assertNotNull(boot.userRoot().findByUsername("test2"));
				assertNull(boot.userRoot().findByUsername("test3"));

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
		try (Tx tx = db.tx()) {
			assertNull(boot.userRoot().findByUsername("test3"));
			assertNotNull("The user with username test2 could not be found.", boot.userRoot().findByUsername("test2"));
		}

	}

	//	@Test
	//	public void testAsyncTrxFailed() throws Throwable {
	//		CompletableFuture<Throwable> cf = new CompletableFuture<>();
	//		db.asyncTrx(() -> {
	//			throw new Exception("kaputt");
	//		}).subscribe(done -> {
	//
	//		} , error -> {
	//			cf.complete(error);
	//		});
	//		assertEquals("kaputt", cf.get().getMessage());
	//	}

	@Test(expected = RuntimeException.class)
	public void testAsyncNoTrxWithError() throws Throwable {
		CompletableFuture<Throwable> cf = new CompletableFuture<>();
		operateNoTx(() -> {
			throw new RuntimeException("error");
		}).toBlocking().value();
		assertEquals("error", cf.get().getMessage());
		throw cf.get();
	}

	@Test
	public void testAsyncNoTrxNestedAsync() throws InterruptedException, ExecutionException {
		String result = operateNoTx(() -> {
			TestUtils.run(() -> {
				TestUtils.sleep(1000);
			});
			return Single.just("OK");
		}).toBlocking().value();
		assertEquals("OK", result);
	}

	//	@Test
	//	public void testAsyncTrxNestedAsync() throws InterruptedException, ExecutionException {
	//		String result = db.asyncTrx(() -> {
	//			TestUtil.run(() -> {
	//				TestUtil.sleep(1000);
	//			});
	//			return "OK";
	//		}).toBlocking().first();
	//		assertEquals("OK", result);
	//	}

	@Test
	public void testAsyncNoTrxSuccess() throws Throwable {
		String result = operateNoTx(() -> {
			return Single.just("OK");
		}).toBlocking().value();
		assertEquals("OK", result);
	}

	// @Test
	// @Ignore
	// public void testUpdateMultithreadedSimple() throws InterruptedException, BrokenBarrierException, TimeoutException {
	//
	// final int nThreads = 10;
	// final int nRuns = 20;
	//
	// for (int r = 0; r < nRuns; r++) {
	// CyclicBarrier barrier = new CyclicBarrier(nThreads);
	// AtomicInteger integer = new AtomicInteger(0);
	//
	// TagFamily tagFamily = tagFamily("colors");
	// Node node = content();
	//
	// ThreadLocal<Boolean> firstTry = new ThreadLocal<>();
	// List<Thread> threads = new ArrayList<>();
	// for (int i = 1; i < nThreads; i++) {
	// System.out.println("Thread [" + i + "] Starting");
	// Thread t = TestUtil.run(() -> {
	// firstTry.set(true);
	// int n = integer.incrementAndGet();
	// db.trx(tx -> {
	// // Load the elements again
	// TagFamily cTagFamily = tagFamily.load();
	// Project cProject = project().load();
	// Node cNode = node.load();
	// User cUser = user().load();
	//
	// Tag tag = cTagFamily.create("bogus_" + n, cProject, cUser);
	// cNode.addTag(tag);
	// tx.complete();
	// if (firstTry.get()) {
	// firstTry.set(false);
	// try {
	// System.out.println("Thread [" + n + "] Waiting..");
	// barrier.await(10, TimeUnit.SECONDS);
	// System.out.println("Thread [" + n + "] Waited");
	// } catch (Exception e) {
	// System.out.println("Thread [" + n + "] Error handling.");
	// e.printStackTrace();
	// }
	// }
	// System.out.println("Thread [" + n + "] Successful updated element.");
	// });
	// });
	// threads.add(t);
	// }
	//
	// System.out.println("Waiting on lock");
	// for (Thread currentThread : threads) {
	// currentThread.join();
	// }
	// try (Trx tx = db.trx()) {
	// int expect = nThreads * (r + 1);
	// assertEquals("Expected {" + expect + "} tags since this is run {" + r + "}", expect, content().getTags().size());
	// }
	// }
	// }

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
			Node node = content();
			TagFamily tagFamily = tagFamily("colors");
			List<Thread> threads = new ArrayList<>();
			Project project = project();
			User user = user();

			for (int i = 0; i < nThreads; i++) {
				final int threadNo = i;
				System.out.println("Thread [" + threadNo + "] Starting");
				Thread t = TestUtils.run(() -> {

					for (int retry = 0; retry < maxRetry; retry++) {
						try {
							try (Tx tx = db.tx()) {

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
								TagFamily reloadedTagFamily = tx.getGraph().getFramedVertexExplicit(TagFamilyImpl.class, tagFamily.getImpl().getId());
								Node reloadedNode = tx.getGraph().getFramedVertexExplicit(NodeImpl.class, node.getImpl().getId());
								User reloadedUser = tx.getGraph().getFramedVertexExplicit(UserImpl.class, user.getImpl().getId());
								Project reloadedProject = tx.getGraph().getFramedVertexExplicit(ProjectImpl.class, project.getImpl().getId());

								Tag tag = reloadedTagFamily.create("bogus_" + threadNo + "_" + currentRun, project(), reloadedUser);
								// Reload the node
								reloadedNode.addTag(tag, reloadedProject.getLatestRelease());
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
			// barrier.await(2, TimeUnit.SECONDS);
			for (Thread currentThread : threads) {
				currentThread.join();
			}
			// Thread.sleep(1000);
			try (Tx tx = db.tx()) {
				int expect = nThreads * (r + 1);
				Node reloadedNode = tx.getGraph().getFramedVertexExplicit(NodeImpl.class, node.getImpl().getId());
				// node.reload();
				assertEquals("Expected {" + expect + "} tags since this is run {" + r + "}.", expect,
						reloadedNode.getTags(project().getLatestRelease()).size());
			}
		}
	}
}
