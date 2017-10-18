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
import com.gentics.mesh.test.context.AbstractMeshTest;
import com.gentics.mesh.test.context.MeshTestSetting;
import com.gentics.mesh.test.util.TestUtils;
import com.syncleus.ferma.tx.Tx;

import io.reactivex.Single;

@MeshTestSetting(useElasticsearch = false, testSize = FULL, startServer = true)
public class TxTest extends AbstractMeshTest {

	@Test
	public void testTransaction() throws InterruptedException {
		AtomicInteger i = new AtomicInteger(0);

		UserRoot root;
		try (Tx tx = tx()) {
			root = meshRoot().getUserRoot();
		}
		int e = i.incrementAndGet();
		try (Tx tx = tx()) {
			assertNotNull(root.create("testuser" + e, user()));
			assertNotNull(boot().userRoot().findByUsername("testuser" + e));
			tx.success();
		}
		try (Tx tx = tx()) {
			assertNotNull(boot().userRoot().findByUsername("testuser" + e));
		}
		int u = i.incrementAndGet();
		Runnable task = () -> {
			try (Tx tx = tx()) {
				assertNotNull(root.create("testuser" + u, user()));
				assertNotNull(boot().userRoot().findByUsername("testuser" + u));
				tx.failure();
			}
			assertNull(boot().userRoot().findByUsername("testuser" + u));

		};
		Thread t = new Thread(task);
		t.start();
		t.join();
		try (Tx tx = tx()) {
			assertNull(boot().userRoot().findByUsername("testuser" + u));
			System.out.println("RUN: " + i.get());
		}

	}

	@Test
	public void testMultiThreadedModifications() throws InterruptedException {
		User user = db().tx(() -> user());

		Runnable task2 = () -> {
			try (Tx tx = tx()) {
				user.setUsername("test2");
				assertNotNull(boot().userRoot().findByUsername("test2"));
				tx.success();
			}
			assertNotNull(boot().userRoot().findByUsername("test2"));

			Runnable task = () -> {
				try (Tx tx = tx()) {
					user.setUsername("test3");
					assertNotNull(boot().userRoot().findByUsername("test3"));
					tx.failure();
				}
				assertNotNull(boot().userRoot().findByUsername("test2"));
				assertNull(boot().userRoot().findByUsername("test3"));

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
			assertNull(boot().userRoot().findByUsername("test3"));
			assertNotNull("The user with username test2 could not be found.", boot().userRoot().findByUsername("test2"));
		}

	}

	@Test(expected = RuntimeException.class)
	public void testAsyncNoTrxWithError() throws Throwable {
		CompletableFuture<Throwable> cf = new CompletableFuture<>();
		db().operateTx(() -> {
			throw new RuntimeException("error");
		}).blockingGet();
		assertEquals("error", cf.get().getMessage());
		throw cf.get();
	}

	@Test
	public void testAsyncNoTrxNestedAsync() throws InterruptedException, ExecutionException {
		String result = db().operateTx(() -> {
			TestUtils.run(() -> {
				TestUtils.sleep(1000);
			});
			return Single.just("OK");
		}).blockingGet();
		assertEquals("OK", result);
	}

	@Test
	public void testAsyncNoTrxSuccess() throws Throwable {
		String result = db().operateTx(() -> {
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
							try (Tx tx = tx()) {

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
								TagFamily reloadedTagFamily = tx.getGraph().getFramedVertexExplicit(TagFamilyImpl.class, tagFamily.getId());
								Node reloadedNode = tx.getGraph().getFramedVertexExplicit(NodeImpl.class, node.getId());
								User reloadedUser = tx.getGraph().getFramedVertexExplicit(UserImpl.class, user.getId());
								Project reloadedProject = tx.getGraph().getFramedVertexExplicit(ProjectImpl.class, project.getId());

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
			try (Tx tx = tx()) {
				int expect = nThreads * (r + 1);
				Node reloadedNode = tx.getGraph().getFramedVertexExplicit(NodeImpl.class, node.getId());
				// node.reload();
				assertEquals("Expected {" + expect + "} tags since this is run {" + r + "}.", expect,
						reloadedNode.getTags(project().getLatestRelease()).size());
			}
		}
	}
}
