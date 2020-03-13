package com.gentics.mesh.core.verticle.handler;

import static com.gentics.mesh.metric.SimpleMetric.WRITE_LOCK_TIMEOUT_COUNT;
import static com.gentics.mesh.metric.SimpleMetric.WRITE_LOCK_WAITING_TIME;

import java.util.concurrent.TimeUnit;

import javax.inject.Inject;
import javax.inject.Singleton;

import com.gentics.mesh.context.InternalActionContext;
import com.gentics.mesh.etc.config.MeshOptions;
import com.gentics.mesh.metric.MetricsService;
import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.core.ILock;

import dagger.Lazy;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Timer;

@Singleton
public class WriteLockImpl implements WriteLock {

	private ILock lock;
	private final MeshOptions options;
	private final Lazy<HazelcastInstance> hazelcast;
	private final Timer writeLockTimer;
	private final Counter timeoutCount;

	@Inject
	public WriteLockImpl(MeshOptions options, Lazy<HazelcastInstance> hazelcast, MetricsService metricsService) {
		this.options = options;
		this.hazelcast = hazelcast;
		this.writeLockTimer = metricsService.timer(WRITE_LOCK_WAITING_TIME);
		this.timeoutCount = metricsService.counter(WRITE_LOCK_TIMEOUT_COUNT);
	}

	@Override
	public void close() {
		if (lock != null) {
			lock.unlock();
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
				try {
					if (lock == null) {
						HazelcastInstance hz = hazelcast.get();
						if (hz != null) {
							this.lock = hz.getLock(WRITE_LOCK_KEY);
						}
					}
					boolean isTimeout = !lock.tryLock(240, TimeUnit.SECONDS);
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
