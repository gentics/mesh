package com.gentics.mesh.core.verticle.handler;

import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

import javax.inject.Inject;
import javax.inject.Singleton;

import com.gentics.mesh.context.InternalActionContext;
import com.gentics.mesh.etc.config.MeshOptions;
import com.gentics.mesh.graphdb.spi.Database;
import com.gentics.mesh.metric.MetricsService;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Timer;

import static com.gentics.mesh.metric.SimpleMetric.WRITE_LOCK_TIMEOUT_COUNT;
import static com.gentics.mesh.metric.SimpleMetric.WRITE_LOCK_WAITING_TIME;

@Singleton
public class WriteLockImpl implements WriteLock {

	private final Semaphore lock = new Semaphore(1);
	private final Database database;
	private final MeshOptions options;
    private final Timer writeLockTimer;
    private final Counter timeoutCount;

    @Inject
	public WriteLockImpl(MeshOptions options, Database database, MetricsService metricsService) {
		this.options = options;
		this.database = database;
        writeLockTimer = metricsService.timer(WRITE_LOCK_WAITING_TIME);
        timeoutCount = metricsService.counter(WRITE_LOCK_TIMEOUT_COUNT);
    }

	@Override
	public void close() {
		lock.release();
	}

	/**
	 * Locks writes. Use this to prevent concurrent write transactions.
	 */
	@Override
	public WriteLock lock(InternalActionContext ac) {
		if (ac.isSkipWriteLock()) {
			return this;
		} else {
			boolean syncWrites = options.getStorageOptions().isSynchronizeWrites();
			database.blockingTopologyLockCheck();
			if (syncWrites) {
                Timer.Sample timer = Timer.start();
                try {
					boolean isTimeout = !lock.tryAcquire(options.getStorageOptions().getSynchronizeWritesTimeout(), TimeUnit.MILLISECONDS);
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

}
