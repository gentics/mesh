package com.gentics.mesh.test;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.junit.rules.Verifier;

import io.reactivex.Flowable;
import io.reactivex.disposables.Disposable;
import okhttp3.ConnectionPool;
import okhttp3.OkHttpClient;

/**
 * Testrule, that verifies that for the given OkHttpClient, the number of used
 * connections in the pool do not change between creation of the instance (at
 * start of the test) and end of the test
 */
public class ConnectionVerifier extends Verifier {
	/**
	 * Connection pool to check
	 */
	protected ConnectionPool connectionPool;

	/**
	 * Number of used connections at creation time
	 */
	protected int usedConnectionsBeforeTest = 0;

	/**
	 * Timeout for waiting for the connection to be freed
	 */
	protected long timeout = 10;

	/**
	 * Unit for the waiting timeout
	 */
	protected TimeUnit timeoutUnit = TimeUnit.SECONDS;

	/**
	 * Create an instance for the given client
	 * @param client client
	 */
	public ConnectionVerifier(OkHttpClient client) {
		this.connectionPool = client.connectionPool();
		usedConnectionsBeforeTest = connectionPool.connectionCount() - connectionPool.idleConnectionCount();
	}

	/**
	 * Set the timeout
	 * @param timeout maximum time to wait
	 * @param timeoutUnit unit
	 * @return fluent API
	 */
	public ConnectionVerifier withTimeout(long timeout, TimeUnit timeoutUnit) {
		this.timeout = timeout;
		this.timeoutUnit = timeoutUnit;
		return this;
	}

	@Override
	protected void verify() throws Throwable {
		waitForConnectionFreed();
		assertThat(getConnectionsUsedByTest()).as("Connections used (and not ended) by test").isEqualTo(0);
	}

	/**
	 * Wait for the connection used by the test to become free
	 * @throws InterruptedException
	 */
	protected void waitForConnectionFreed() throws InterruptedException {
		// now wait for the connection to be freed
		CountDownLatch waitLatch = new CountDownLatch(1);
		Disposable disp = Flowable.interval(100, TimeUnit.MILLISECONDS).forEach(ignore -> {
			if (getConnectionsUsedByTest() == 0) {
				waitLatch.countDown();
			}
		});
		try {
			waitLatch.await(timeout, timeoutUnit);
		} finally {
			disp.dispose();
		}
	}

	/**
	 * Determine the number of connections used by the test
	 * @return number of used connections
	 */
	protected int getConnectionsUsedByTest() {
		int usedConnectionsAfterTest = connectionPool.connectionCount() - connectionPool.idleConnectionCount();
		return usedConnectionsAfterTest - usedConnectionsBeforeTest;
	}
}
