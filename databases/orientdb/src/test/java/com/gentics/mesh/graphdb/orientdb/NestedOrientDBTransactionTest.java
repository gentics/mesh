package com.gentics.mesh.graphdb.orientdb;

import static org.junit.Assert.assertEquals;

import java.util.concurrent.CountDownLatch;

import org.apache.tinkerpop.gremlin.orientdb.OrientGraph;
import org.apache.tinkerpop.gremlin.orientdb.OrientGraphFactory;
import org.junit.Test;

public class NestedOrientDBTransactionTest {

	OrientGraphFactory factory = new OrientGraphFactory("memory:tinkerpop").setupPool(4, 10);

	/**
	 * Verify that nested transactions are handled correctly. The outer transaction should not commit when the inner transaction is started.
	 */
	@Test
	public void testNestedTransaction() {
		try {
			OrientGraph tx = factory.getTx();
			try {
				tx.addVertex(null);
				CountDownLatch latch = new CountDownLatch(1);

				new Thread(() -> {
					OrientGraph tx2 = factory.getTx();
					try {
						long count = tx.countVertices();
						System.out.println("Inner " + count);
					} finally {
						tx2.close();
					}
					latch.countDown();
				}).start();
				latch.await();
				throw new RuntimeException();
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} finally {
				tx.rollback();
				tx.close();
			}
		} catch (RuntimeException e) {
			e.printStackTrace();
		}
		OrientGraph tx = factory.getTx();
		try {
			long count = tx.countVertices();
			assertEquals("A runtime exception occured in the tx transaction. Nothing should have been comitted", 0, count);
		} finally {
			tx.close();
		}

	}

	/**
	 * Verify that nested transactions are handled correctly. The outer transaction should not commit when the inner transaction is started.
	 */
	@Test
	public void testNestedTransaction2() {
		try {
			OrientGraph tx = factory.getTx();
			try {
				
				// 1. Add a vertex in the inner transaction
				tx.addVertex(null);

				OrientGraph tx2 = factory.getTx();
				try {
					// Verify that the inner count is 1
					long count = tx2.countVertices();
					System.out.println("Inner " + count);
				} finally {
					tx2.rollback();
					tx2.close();
				}
				throw new RuntimeException();
			} finally {
				tx.rollback();
				tx.close();
			}
		} catch (RuntimeException e) {
			e.printStackTrace();
		}
		OrientGraph tx = factory.getTx();
		try {
			long count = tx.countVertices();
			assertEquals("A runtime exception occured in the tx transaction. Nothing should have been comitted", 0, count);
		} finally {
			tx.close();
		}

	}

}
