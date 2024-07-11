package com.gentics.mesh.database;

import java.util.concurrent.atomic.AtomicLong;

import com.gentics.mesh.database.connector.DatabaseConnector;
import com.gentics.mesh.hibernate.util.QueryCounter;
import com.gentics.mesh.json.JsonUtil;
import com.gentics.mesh.test.MeshTestSetting;
import com.gentics.mesh.test.TestSize;
import com.gentics.mesh.test.context.AbstractMeshTest;
import com.gentics.mesh.test.context.ClientHandler;

/**
 * Test cases which count the number of executed queries (and assert that the number does not exceed a given limit). Please parameterize the implementors, as shown below!
 */
@MeshTestSetting(testSize = TestSize.PROJECT, monitoring = true, startServer = true, customOptionChanger = QueryCounter.EnableHibernateJmxExposure.class, resetBetweenTests = false)
public abstract class AbstractCountingTest extends AbstractMeshTest {

	/**
	 * Set this flag to true for debugging. The test will then output changes in the number of executed queries
	 */
	public final static boolean DEBUG = true;

	/**
	 * Execute the given handler and assert that no more than either {@link #ALLOWED_WITH_ETAG} or {@link #ALLOWED_NO_ETAG} queries were executed by Hibernate.
	 * @param <T> type of response of handler
	 * @param handler test handler
	 */
	protected <T> T doTest(ClientHandler<T> handler, int noMoreThan) {
		DatabaseConnector dc = tx(tx -> {
			return tx.<HibernateTx>unwrap().data().getDatabaseConnector();
		});
		try (QueryCounter queryCounter = QueryCounter.Builder.get().withDatabaseConnector(dc).clear().assertNotMoreThan(noMoreThan).build()) {
			long periodicId = 0;
			if (DEBUG) {
				AtomicLong currentCount = new AtomicLong();
				periodicId = vertx().setPeriodic(1000, id -> {
					long newCount = queryCounter.getCountSinceStart();
					if (currentCount.get() != newCount) {
						currentCount.set(newCount);
						System.out.println(String.format("Current diff: %d", currentCount.get()));
						System.out.println(queryCounter.getQueries());
					}
				});
			}

			T result = nonAdminCall(handler);
			if (DEBUG) {
				System.err.println(JsonUtil.toJson(result, false));
				vertx().cancelTimer(periodicId);
			}
			return result;
		}
	}
}
