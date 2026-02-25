package com.gentics.mesh.database;

import static org.assertj.core.api.Assertions.fail;

import java.util.concurrent.Callable;
import java.util.concurrent.atomic.AtomicLong;

import com.gentics.mesh.core.data.Bucket;
import com.gentics.mesh.core.db.TxAction;
import com.gentics.mesh.database.connector.DatabaseConnector;
import com.gentics.mesh.hibernate.util.QueryCounter;
import com.gentics.mesh.json.JsonUtil;
import com.gentics.mesh.test.MeshTestSetting;
import com.gentics.mesh.test.ResetTestDb;
import com.gentics.mesh.test.TestSize;
import com.gentics.mesh.test.context.AbstractMeshTest;
import com.gentics.mesh.test.context.ClientHandler;

/**
 * Test cases which count the number of executed queries (and assert that the number does not exceed a given limit). Please parameterize the implementors, as shown below!
 */
@MeshTestSetting(testSize = TestSize.PROJECT, monitoring = true, startServer = true, customOptionChanger = QueryCounter.EnableHibernateStatistics.class, resetBetweenTests = ResetTestDb.NEVER)
public abstract class AbstractCountingTest extends AbstractMeshTest {
	public final static Bucket ONLY_BUCKET = new Bucket(0, Integer.MAX_VALUE, 0, 1);

	/**
	 * Set this flag to true for debugging. The test will then output changes in the number of executed queries
	 */
	public final static boolean DEBUG = false;

	/**
	 * Execute the given handler and assert that no more than either {@link #ALLOWED_WITH_ETAG} or {@link #ALLOWED_NO_ETAG} queries were executed by Hibernate.
	 * @param <T> type of response of handler
	 * @param handler test handler
	 * @param noMoreThan maximum allowed queries
	 */
	protected <T> T doTest(ClientHandler<T> handler, int noMoreThan) {
		return doTest(handler, noMoreThan, -1);
	}

	/**
	 * Execute the given handler and assert that no more than either {@link #ALLOWED_WITH_ETAG} or {@link #ALLOWED_NO_ETAG} queries were executed by Hibernate.
	 * @param <T> type of response of handler
	 * @param handler test handler
	 * @param noMoreThan maximum allowed queries
	 * @param atLeast minimum number of expected queries
	 */
	protected <T> T doTest(ClientHandler<T> handler, int noMoreThan, int atLeast) {
		DatabaseConnector dc = tx(tx -> {
			return tx.<HibernateTx>unwrap().data().getDatabaseConnector();
		});
		try (QueryCounter queryCounter = QueryCounter.Builder.get().withDatabaseConnector(dc).clear()
				.assertNotMoreThan(noMoreThan).assertAtLeast(atLeast).build()) {
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

	protected <T> T doTest(TxAction<T> handler, int noMoreThan, int atLeast) {
		DatabaseConnector dc = tx(tx -> {
			return tx.<HibernateTx>unwrap().data().getDatabaseConnector();
		});

		try (QueryCounter queryCounter = QueryCounter.Builder.get().withDatabaseConnector(dc).clear()
				.assertNotMoreThan(noMoreThan).assertAtLeast(atLeast).build()) {
			return tx(handler);
		}
	}

	protected <T> T doTest(Callable<T> handler, int noMoreThan, int atLeast) {
		DatabaseConnector dc = tx(tx -> {
			return tx.<HibernateTx>unwrap().data().getDatabaseConnector();
		});
		try (QueryCounter queryCounter = QueryCounter.Builder.get().withDatabaseConnector(dc).clear()
				.assertNotMoreThan(noMoreThan).assertAtLeast(atLeast).build()) {
			try {
				return handler.call();
			} catch (Exception e) {
				fail("Tested method failed", e);
				return null;
			}
		}
	}

	protected void doTest(Runnable handler, int noMoreThan, int atLeast) {
		DatabaseConnector dc = tx(tx -> {
			return tx.<HibernateTx>unwrap().data().getDatabaseConnector();
		});
		try (QueryCounter queryCounter = QueryCounter.Builder.get().withDatabaseConnector(dc).clear()
				.assertNotMoreThan(noMoreThan).assertAtLeast(atLeast).build()) {
			try {
				handler.run();
			} catch (Exception e) {
				fail("Tested method failed", e);
			}
		}
	}
}
