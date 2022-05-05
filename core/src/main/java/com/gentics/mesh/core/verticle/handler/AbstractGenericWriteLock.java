package com.gentics.mesh.core.verticle.handler;

import static com.gentics.mesh.metric.SimpleMetric.WRITE_LOCK_TIMEOUT_COUNT;
import static com.gentics.mesh.metric.SimpleMetric.WRITE_LOCK_WAITING_TIME;

import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

import com.gentics.mesh.context.InternalActionContext;
import com.gentics.mesh.etc.config.MeshOptions;
import com.gentics.mesh.metric.MetricsService;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Timer;

/**
 * Generic application-based implementation of the WriteLock.
 * 
 * @author plyhun
 *
 */
public abstract class AbstractGenericWriteLock implements WriteLock {

	protected final MeshOptions options;
	protected final Timer writeLockTimer;
	protected final Counter timeoutCount;
	protected final Semaphore localLock = new Semaphore(1);

	public AbstractGenericWriteLock(MeshOptions options, MetricsService metricsService) {
		this.options = options;
		this.writeLockTimer = metricsService.timer(WRITE_LOCK_WAITING_TIME);
		this.timeoutCount = metricsService.counter(WRITE_LOCK_TIMEOUT_COUNT);
	}

	/**
	 * Get the transaction timeout
	 * 
	 * @return
	 */
	abstract protected long getSyncWritesTimeoutMillis();

	@Override
	public void close() {
		localLock.release();
	}

	/**
	 * Locks writes. Use this to prevent concurrent write transactions.
	 */
	@Override
	public WriteLock lock(InternalActionContext ac) {
		if (isSyncWrites() && (ac == null || !ac.isSkipWriteLock())) {
			Timer.Sample timer = Timer.start();
			long timeout = getSyncWritesTimeoutMillis();
			try {
				boolean isTimeout = !localLock.tryAcquire(timeout, TimeUnit.MILLISECONDS);
				if (isTimeout) {
					timeoutCount.increment();
					throw new RuntimeException("Got timeout while waiting for write lock.");
				}
			} catch (InterruptedException e) {
				throw new RuntimeException(e);
			} finally {
				timer.stop(writeLockTimer);
			}
		}
		return this;
	}
}
