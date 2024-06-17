package com.gentics.mesh.graphdb.orientdb;

import static org.junit.Assert.assertEquals;

import org.junit.Before;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;

import com.gentics.mesh.core.data.util.HibClassConverter;
import com.gentics.mesh.core.db.Database;
import com.gentics.mesh.core.db.GraphDBTx;
import com.gentics.mesh.etc.config.OrientDBMeshOptions;
import com.gentics.mesh.mock.MockingLoggerRule;
import com.tinkerpop.blueprints.Vertex;

import io.vertx.core.spi.logging.LogDelegate;

@Ignore
public class NestedTransactionTest extends AbstractOrientDBTest {

	private Database db;

	@Rule
	public MockingLoggerRule rule = new MockingLoggerRule();

	protected LogDelegate logger = rule.get(NestedTransactionTest.class.getName());

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
				logger.info("Outer");
				db.tx((tx2) -> {
					GraphDBTx gtx2 = HibClassConverter.toGraph(tx2);
					long count = gtx2.getGraph().v().count();
					logger.info("Inner " + count);
				});
				logger.info("Outer Done");
				if (true == false) {
					// Added to make Java type system less frantic in picking the method with correct signature
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
