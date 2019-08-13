package com.gentics.madl.tx;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.mockito.Mockito.verify;

import org.junit.Test;
import org.mockito.Mockito;

import com.gentics.madl.tx.AbstractTx;
import com.gentics.madl.tx.Tx;
import com.gentics.madl.tx.TxAction;
import com.gentics.madl.tx.TxFactory;
import com.syncleus.ferma.FramedTransactionalGraph;

public class TxFactoryTest implements TxFactory {

	private Tx mock = Mockito.mock(Tx.class);

	@Test
	public void testTx0() {
		try (Tx tx = tx()) {

		}
		verify(mock).close();
	}

	@Test
	public void testTx1() {
		tx(() -> {

		});
		verify(mock).close();
	}

	@Test
	public void testTx2() {
		assertEquals("test", tx(() -> {
			return "test";
		}));
		verify(mock).close();
	}

	@Test
	public void testTx3() {
		assertEquals("test", tx((tx) -> {
			tx.failure();
			tx.success();
			return "test";
		}));
		verify(mock).close();
	}

	@Test
	public void testAbstractTxSucceeding() {
		@SuppressWarnings("unchecked")
		AbstractTx<FramedTransactionalGraph> tx = Mockito.mock(AbstractTx.class, Mockito.CALLS_REAL_METHODS);
		FramedTransactionalGraph graph = Mockito.mock(FramedTransactionalGraph.class);
		tx.init(graph);
		try (Tx tx2 = tx) {
			assertNotNull(Tx.get());
			tx2.success();
		}
		assertNull(Tx.get());
		verify(tx).close();
		verify(graph).commit();
		verify(graph).close();
		verify(graph).shutdown();
		verify(graph, Mockito.never()).rollback();
	}

	@Test
	public void testAbstractTxDefault() {
		@SuppressWarnings("unchecked")
		AbstractTx<FramedTransactionalGraph> tx = Mockito.mock(AbstractTx.class, Mockito.CALLS_REAL_METHODS);
		FramedTransactionalGraph graph = Mockito.mock(FramedTransactionalGraph.class);
		tx.init(graph);
		try (Tx tx2 = tx) {
			assertNotNull(Tx.get());
			// Don't call tx2.success() or tx2.failure()
		}
		assertNull(Tx.get());
		verify(tx).close();
		verify(graph).rollback();
		verify(graph).close();
		verify(graph).shutdown();
		verify(graph, Mockito.never()).commit();
	}

	@Test
	public void testAbstractTxFailing() {
		@SuppressWarnings("unchecked")
		AbstractTx<FramedTransactionalGraph> tx = Mockito.mock(AbstractTx.class, Mockito.CALLS_REAL_METHODS);
		FramedTransactionalGraph graph = Mockito.mock(FramedTransactionalGraph.class);
		tx.init(graph);
		try (Tx tx2 = tx) {
			assertNotNull(Tx.get());
			tx2.failure();
		}
		assertNull(Tx.get());
		verify(tx).close();
		verify(graph).rollback();
		verify(graph).close();
		verify(graph).shutdown();
		verify(graph, Mockito.never()).commit();
	}

	@Override
	public Tx tx() {
		return mock;
	}

	@Override
	public <T> T tx(TxAction<T> txHandler) {
		try (Tx tx = tx()) {
			try {
				return txHandler.handle(mock);
			} catch (Exception e) {
				throw new RuntimeException(e);
			}
		}
	}

}
