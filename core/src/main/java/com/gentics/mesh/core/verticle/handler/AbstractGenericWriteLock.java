package com.gentics.mesh.core.verticle.handler;

import static com.gentics.mesh.core.rest.error.Errors.error;
import static com.gentics.mesh.metric.SimpleMetric.WRITE_LOCK_TIMEOUT_COUNT;
import static com.gentics.mesh.metric.SimpleMetric.WRITE_LOCK_WAITING_TIME;
import static io.netty.handler.codec.http.HttpResponseStatus.SERVICE_UNAVAILABLE;

import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

import com.gentics.mesh.context.InternalActionContext;
import com.gentics.mesh.core.db.cluster.ClusterManager;
import com.gentics.mesh.etc.config.MeshOptions;
import com.gentics.mesh.metric.MetricsService;
import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.core.ILock;

import dagger.Lazy;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Timer;

/**
 * Generic application-based implementation of the WriteLock.
 * 
 * @author plyhun
 *
 */
public abstract class AbstractGenericWriteLock implements WriteLock {

	protected ILock clusterLock;
	protected final Semaphore localLock = new Semaphore(1);
	protected final MeshOptions options;
	protected final Lazy<HazelcastInstance> hazelcast;
	protected final boolean isClustered;
	protected final Timer writeLockTimer;
	protected final Counter timeoutCount;
	protected final ClusterManager clusterManager;

	public AbstractGenericWriteLock(MeshOptions options, Lazy<HazelcastInstance> hazelcast, MetricsService metricsService, ClusterManager clusterManager) {
		this.options = options;
		this.hazelcast = hazelcast;
		this.isClustered = options.getClusterOptions().isEnabled();
		this.writeLockTimer = metricsService.timer(WRITE_LOCK_WAITING_TIME);
		this.timeoutCount = metricsService.counter(WRITE_LOCK_TIMEOUT_COUNT);
		this.clusterManager = clusterManager;
	}

	/**
	 * Are the writes to DB synchronozed?
	 * 
	 * @return
	 */
	protected boolean isSyncWrites() {
		return true;
	}

	/**
	 * Get the transaction timeout
	 * 
	 * @return
	 */
	abstract protected long getSyncWritesTimeoutMillis();

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
	public WriteLock lock(InternalActionContext ac) {
		if (ac != null && ac.isSkipWriteLock()) {
			return this;
		} else {
			// throw an error, if the cluster topology is currently locked and the option "topology change readonly" is activated
			if (options.getClusterOptions().isTopologyChangeReadOnly() && clusterManager != null
					&& clusterManager.isClusterTopologyLocked()) {
				throw error(SERVICE_UNAVAILABLE, "error_cluster_topology_readonly").setLogStackTrace(false);
			}

			if (isSyncWrites()) {
				Timer.Sample timer = Timer.start();
				long timeout = getSyncWritesTimeoutMillis();
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
