package com.gentics.mesh.graphdb.orientdb;

import static org.junit.Assert.*;

import org.junit.Before;
import org.junit.Test;

import com.gentics.mesh.graphdb.OrientDBDatabase;
import com.gentics.mesh.graphdb.spi.Database;
import com.tinkerpop.blueprints.Vertex;

import io.vertx.core.Vertx;
import io.vertx.core.logging.LoggerFactory;
import io.vertx.core.logging.SLF4JLogDelegateFactory;

public class NestedTransactionTest {

	private Database db = new OrientDBDatabase();

	static {
		// Use slf4j instead of jul
		System.setProperty(LoggerFactory.LOGGER_DELEGATE_FACTORY_CLASS_NAME, SLF4JLogDelegateFactory.class.getName());
		LoggerFactory.getLogger(NestedTransactionTest.class);
	}

	@Before
	public void setup() throws Exception {
		db.init(null, Vertx.vertx());
	}

	/**
	 * Verify that nested transactions are handled correctly. The outer transaction should not commit when the inner transaction is started.
	 */
	@Test
	public void testNestedTransaction() {
		try {
			db.tx(() -> {
				Vertex v = Database.getThreadLocalGraph().addVertex(null);
				System.out.println("Outer");
				db.noTx(() -> {
					long count = Database.getThreadLocalGraph().v().count();
					System.out.println("Inner " + count);
					return null;
				});
				System.out.println("Outer Done");
				throw new RuntimeException();
				//			return null;
			});
		} catch (RuntimeException e) {
			e.printStackTrace();
		}
		long count = db.noTx(() -> Database.getThreadLocalGraph().v().count());
		assertEquals("A runtime exception occured in the tx transaction. Nothing should have been comitted", 0, count);
	}

}
