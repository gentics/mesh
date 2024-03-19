package com.gentics.mesh.graphdb.arcadedb;

import static org.junit.Assert.assertEquals;

import java.util.concurrent.CountDownLatch;

import org.junit.Test;

import com.arcadedb.gremlin.ArcadeGraph;
import com.arcadedb.gremlin.ArcadeGraphFactory;
import com.gentics.mesh.util.StreamUtil;

public class NestedArcadeDBTransactionTest {

	ArcadeGraphFactory factory = ArcadeGraphFactory.withLocal("memory:tinkerpop");

	/**
	 * Verify that nested transactions are handled correctly. The outer transaction should not commit when the inner transaction is started.
	 */
	@Test
	public void testNestedTransaction() {
		try {
			ArcadeGraph tx = factory.get();
			try {
				tx.addVertex();
				CountDownLatch latch = new CountDownLatch(1);

				new Thread(() -> {
					try (ArcadeGraph tx2 = factory.get()) {
						long count = StreamUtil.toStream(tx.vertices()).count();
						System.out.println("Inner " + count);
					}
					latch.countDown();
				}).start();
				latch.await();
				throw new RuntimeException();
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} finally {
				tx.tx().rollback();
				tx.close();
			}
		} catch (RuntimeException e) {
			e.printStackTrace();
		}
		;
		try (ArcadeGraph tx = factory.get()) {
			long count = StreamUtil.toStream(tx.vertices()).count();
			assertEquals("A runtime exception occured in the tx transaction. Nothing should have been comitted", 0, count);
		}
	}

	/**
	 * Verify that nested transactions are handled correctly. The outer transaction should not commit when the inner transaction is started.
	 */
	@Test
	public void testNestedTransaction2() {
		try {
			ArcadeGraph tx = factory.get();
			try {				
				// 1. Add a vertex in the inner transaction
				tx.addVertex();

				ArcadeGraph tx2 = factory.get();
				try {
					// Verify that the inner count is 1
					long count = StreamUtil.toStream(tx2.vertices()).count();
					System.out.println("Inner " + count);
				} finally {
					tx2.tx().rollback();
					tx2.close();
				}
				throw new RuntimeException();
			} finally {
				tx.tx().rollback();
				tx.close();
			}
		} catch (RuntimeException e) {
			e.printStackTrace();
		}
		try (ArcadeGraph tx = factory.get()) {
			long count = StreamUtil.toStream(tx.vertices()).count();
			assertEquals("A runtime exception occured in the tx transaction. Nothing should have been comitted", 0, count);
		}
	}

}
