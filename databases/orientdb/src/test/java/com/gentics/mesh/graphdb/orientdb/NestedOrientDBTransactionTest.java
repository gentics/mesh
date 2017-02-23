package com.gentics.mesh.graphdb.orientdb;

import static org.junit.Assert.assertEquals;

import java.util.concurrent.CountDownLatch;

import org.junit.Test;

import com.tinkerpop.blueprints.impls.orient.OrientGraph;
import com.tinkerpop.blueprints.impls.orient.OrientGraphFactory;
import com.tinkerpop.blueprints.impls.orient.OrientGraphNoTx;

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
					OrientGraphNoTx noTx = factory.getNoTx();
					try {
						long count = noTx.countVertices();
						System.out.println("Inner " + count);
					} finally {
						noTx.shutdown();
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
				tx.shutdown();
			}
		} catch (RuntimeException e) {
			e.printStackTrace();
		}
		OrientGraphNoTx noTx = factory.getNoTx();
		try {
			long count = noTx.countVertices();
			assertEquals("A runtime exception occured in the tx transaction. Nothing should have been comitted", 0, count);
		} finally {
			noTx.shutdown();
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
				tx.addVertex(null);

				OrientGraph tx2 = factory.getTx();
				try {
					//tx2.addVertex(null);
					long count = tx2.countVertices();
					System.out.println("Inner " + count);
				} finally {
					//tx2.rollback();
					tx2.shutdown();
				}
				throw new RuntimeException();
			} finally {
				tx.rollback();
				tx.shutdown();
			}
		} catch (RuntimeException e) {
			e.printStackTrace();
		}
		OrientGraphNoTx noTx = factory.getNoTx();
		try {
			long count = noTx.countVertices();
			assertEquals("A runtime exception occured in the tx transaction. Nothing should have been comitted", 0, count);
		} finally {
			noTx.shutdown();
		}

	}

}
