package com.gentics.mesh.graphdb.orientdb;

import static org.junit.Assert.assertEquals;

import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

import com.gentics.mesh.core.data.util.HibClassConverter;
import com.gentics.mesh.core.db.Database;
import com.gentics.mesh.core.db.GraphDBTx;
import com.gentics.mesh.etc.config.OrientDBMeshOptions;
import com.tinkerpop.blueprints.Vertex;

import io.vertx.core.logging.LoggerFactory;
import io.vertx.core.logging.SLF4JLogDelegateFactory;

@Ignore
public class NestedTransactionTest extends AbstractOrientDBTest {

	private Database db;
	

	static {
		// Use slf4j instead of jul
		System.setProperty(LoggerFactory.LOGGER_DELEGATE_FACTORY_CLASS_NAME, SLF4JLogDelegateFactory.class.getName());
		LoggerFactory.getLogger(NestedTransactionTest.class);
	}

	@Before
	public void setup() throws Exception {
		db = mockDatabase(new OrientDBMeshOptions());
		db.init(null);
	}

	/**
	 * Verify that nested transactions are handled correctly. The outer transaction should not commit when the inner transaction is started.
	 */
	@Test
	public void testNestedTransaction() {
		try {
			db.tx((tx) -> {
				GraphDBTx gtx = HibClassConverter.toGraph(tx);
				Vertex v = gtx.getGraph().addVertex(null);
				System.out.println("Outer");
				db.tx((tx2) -> {
					GraphDBTx gtx2 = HibClassConverter.toGraph(tx2);
					long count = gtx2.getGraph().v().count();
					System.out.println("Inner " + count);
				});
				System.out.println("Outer Done");
				if (true == false) {
					return null;
				}
				throw new RuntimeException();
			});
		} catch (RuntimeException e) {
			e.printStackTrace();
		}
		long count = db.tx((tx) -> {
			GraphDBTx gtx = HibClassConverter.toGraph(tx);
			return gtx.getGraph().v().count();
		});
		assertEquals("A runtime exception occured in the tx transaction. Nothing should have been comitted", 0, count);
	}

}
