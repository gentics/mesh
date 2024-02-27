package com.gentics.madl.tx;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.mockito.Mockito.verify;

import java.io.IOException;

import org.apache.tinkerpop.gremlin.structure.Graph;
import org.junit.Test;
import org.mockito.Mockito;

import com.gentics.madl.graph.DelegatingFramedMadlGraph;

public class TxFactoryTest implements TxFactory {

	private Tx mock = Mockito.mock(Tx.class);

	@Test
	public void testTx0() throws IOException {
		try (Tx tx = tx()) {

		}
		verify(mock).close();
	}

	@Test
	public void testTx1() throws IOException {
		tx(() -> {

		});
		verify(mock).close();
	}

	@Test
	public void testTx2() throws IOException {
		assertEquals("test", tx(() -> {
			return "test";
		}));
		verify(mock).close();
	}

	@Test
	public void testTx3() throws IOException {
		assertEquals("test", tx((tx) -> {
			tx.failure();
			tx.success();
			return "test";
		}));
		verify(mock).close();
	}

	@Test
	public void testAbstractTxSucceeding() throws IOException {
		@SuppressWarnings("unchecked")
		AbstractTx<? extends Graph, DelegatingFramedMadlGraph<? extends Graph>> tx = Mockito.mock(AbstractTx.class, Mockito.CALLS_REAL_METHODS);
		DelegatingFramedMadlGraph<? extends Graph> graph = Mockito.mock(DelegatingFramedMadlGraph.class);
		tx.init(graph);
		try (Tx tx2 = tx) {
			assertNotNull(Tx.get());
			tx2.success();
		}
		assertNull(Tx.get());
		verify(tx).close();
		verify(graph).tx().commit();
		verify(graph).close();
		//verify(graph).shutdown();
		verify(graph, Mockito.never()).tx().rollback();
	}

	@Test
	public void testAbstractTxDefault() throws IOException {
		@SuppressWarnings("unchecked")
		AbstractTx<? extends Graph, DelegatingFramedMadlGraph<? extends Graph>> tx = Mockito.mock(AbstractTx.class, Mockito.CALLS_REAL_METHODS);
		DelegatingFramedMadlGraph<? extends Graph> graph = Mockito.mock(DelegatingFramedMadlGraph.class);
		tx.init(graph);
		try (Tx tx2 = tx) {
			assertNotNull(Tx.get());
			// Don't call tx2.success() or tx2.failure()
		}
		assertNull(Tx.get());
		verify(tx).close();
		verify(graph).tx().rollback();
		verify(graph).close();
		//verify(graph).tx().shutdown();
		verify(graph, Mockito.never()).tx().commit();
	}

	@Test
	public void testAbstractTxFailing() throws IOException {
		@SuppressWarnings("unchecked")
		AbstractTx<? extends Graph, DelegatingFramedMadlGraph<? extends Graph>> tx = Mockito.mock(AbstractTx.class, Mockito.CALLS_REAL_METHODS);
		DelegatingFramedMadlGraph<? extends Graph> graph = Mockito.mock(DelegatingFramedMadlGraph.class);
		tx.init(graph);
		try (Tx tx2 = tx) {
			assertNotNull(Tx.get());
			tx2.failure();
		}
		assertNull(Tx.get());
		verify(tx).close();
		verify(graph).tx().rollback();
		verify(graph).close();
		//verify(graph).tx().shutdown();
		verify(graph, Mockito.never()).tx().commit();
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
		} catch (IOException e1) {
			throw new RuntimeException(e1);
		}
	}

}
