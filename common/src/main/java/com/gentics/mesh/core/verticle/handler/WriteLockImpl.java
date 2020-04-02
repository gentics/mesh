package com.gentics.mesh.core.verticle.handler;

import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

import javax.inject.Inject;
import javax.inject.Singleton;

import com.gentics.mesh.context.InternalActionContext;
import com.gentics.mesh.etc.config.MeshOptions;
import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.core.ILock;

import dagger.Lazy;

@Singleton
public class WriteLockImpl implements WriteLock {

	private ILock lock;
	private final Semaphore localLock = new Semaphore(1);
	private final MeshOptions options;
	private final Lazy<HazelcastInstance> hazelcast;
	private final boolean isClustered;
	private final Counter timeoutCount;

	@Inject
	public WriteLockImpl(MeshOptions options, Lazy<HazelcastInstance> hazelcast, MetricsService metricsService) {
		this.options = options;
		this.hazelcast = hazelcast;
		this.isClustered = options.getClusterOptions().isEnabled();
		this.writeLockTimer = metricsService.timer(WRITE_LOCK_WAITING_TIME);
		this.timeoutCount = metricsService.counter(WRITE_LOCK_TIMEOUT_COUNT);
	}

	@Override
	public void close() {
		if (isClustered) {
			if (clusterLock != null) {
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
	public WriteLock lock(InternalActionContext ac) {
		if (ac != null && ac.isSkipWriteLock()) {
			return this;
		} else {
			boolean syncWrites = options.getStorageOptions().isSynchronizeWrites();
			if (syncWrites) {
				Timer.Sample timer = Timer.start();
				long timeout = options.getStorageOptions().getSynchronizeWritesTimeout();
				if (isClustered) {
					try {
						if (clusterLock == null) {
							HazelcastInstance hz = hazelcast.get();
							if (hz != null) {
								this.clusterLock = hz.getLock(WRITE_LOCK_KEY);
							}
						}
						boolean isTimeout = !clusterLock.tryLock(timeout, TimeUnit.MILLISECONDS);
						if (isTimeout) {
							timeoutCount.increment();
							throw new RuntimeException("Got timeout while waiting for write lock.");
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
