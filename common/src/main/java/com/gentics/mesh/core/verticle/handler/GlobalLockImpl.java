package com.gentics.mesh.core.verticle.handler;

import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

import javax.inject.Inject;
import javax.inject.Singleton;

import com.gentics.mesh.context.InternalActionContext;
import com.gentics.mesh.etc.config.MeshOptions;
import com.gentics.mesh.metric.MetricsService;
import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.core.ILock;
import static com.gentics.mesh.metric.SimpleMetric.WRITE_LOCK_TIMEOUT_COUNT;
import static com.gentics.mesh.metric.SimpleMetric.WRITE_LOCK_WAITING_TIME;

import dagger.Lazy;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Timer;

@Singleton
public class GlobalLockImpl implements GlobalLock {

	private ILock clusterLock;
	private final Semaphore localLock = new Semaphore(1);
	private final MeshOptions options;
	private final Lazy<HazelcastInstance> hazelcast;
	private final boolean isClustered;
	private final Timer writeLockTimer;
	private final Counter timeoutCount;

	@Inject
	public GlobalLockImpl(MeshOptions options, Lazy<HazelcastInstance> hazelcast, MetricsService metricsService) {
		this.options = options;
		this.hazelcast = hazelcast;
		this.isClustered = options.getClusterOptions().isEnabled();
		this.writeLockTimer = metricsService.timer(WRITE_LOCK_WAITING_TIME);
		this.timeoutCount = metricsService.counter(WRITE_LOCK_TIMEOUT_COUNT);
	}

	@Override
	public void close() {
		if (isClustered) {
			if (clusterLock != null && clusterLock.isLockedByCurrentThread()) {
				clusterLock.unlock();
			}
		} else {
			localLock.release();
		}
	}

	/**
	 * Locks writes. Use this to prevent concurrent write transactions.
	 */
	@Override
	public GlobalLock writeLock(InternalActionContext ac) {
		return lock(ac);
	}

	private GlobalLock lock(InternalActionContext ac) {
		if (ac != null && ac.isSkipWriteLock()) {
			return this;
		} else {
			// Lets mark this ac as being used by a lock. This way no other nested lock will cause a deadlock.
			if (ac != null) {
				ac.skipWriteLock();
			}
			boolean syncWrites = options.getStorageOptions().isSynchronizeWrites();
			boolean syncReads = true;
			if (syncWrites || syncReads) {
				Timer.Sample timer = Timer.start();
				long timeout = options.getStorageOptions().getSynchronizeWritesTimeout();
				if (isClustered) {
					try {
						if (clusterLock == null) {
							HazelcastInstance hz = hazelcast.get();
							if (hz != null) {
								this.clusterLock = hz.getLock(GLOBAL_LOCK_KEY);
							}
						}
						if (clusterLock != null) {
							boolean isTimeout = !clusterLock.tryLock(timeout, TimeUnit.MILLISECONDS);
							if (isTimeout) {
								timeoutCount.increment();
								throw new RuntimeException("Got timeout while waiting for write lock.");
							}
						}
					} catch (InterruptedException e) {
						throw new RuntimeException(e);
					} finally {
						timer.stop(writeLockTimer);
					}
				} else {
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
			}
			return this;
		}
	}

}
