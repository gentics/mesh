package com.gentics.mesh.core.db;

import static com.gentics.mesh.metric.SimpleMetric.TOPOLOGY_LOCK_TIMEOUT_COUNT;
import static com.gentics.mesh.metric.SimpleMetric.TOPOLOGY_LOCK_WAITING_TIME;
import static com.gentics.mesh.metric.SimpleMetric.TX_RETRY;
import static com.gentics.mesh.metric.SimpleMetric.TX_TIME;

import com.gentics.mesh.Mesh;
import com.gentics.mesh.MeshStatus;
import com.gentics.mesh.metric.MetricsService;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Timer;

/**
 * Common functionality of a {@link Database}
 * 
 * @author plyhun
 *
 */
public abstract class CommonDatabase implements Database {

	protected final Mesh mesh;
	protected final MetricsService metrics;

	protected Timer txTimer;
	protected Counter txRetryCounter;
	protected Timer topologyLockTimer;
	protected Counter topologyLockTimeoutCounter;

	protected CommonDatabase(Mesh mesh, MetricsService metrics) {
		this.mesh = mesh;
		this.metrics = metrics;
		if (metrics != null) {
			txTimer = metrics.timer(TX_TIME);
			txRetryCounter = metrics.counter(TX_RETRY);
			topologyLockTimer = metrics.timer(TOPOLOGY_LOCK_WAITING_TIME);
			topologyLockTimeoutCounter = metrics.counter(TOPOLOGY_LOCK_TIMEOUT_COUNT);
		}
	}

	/**
	 * Check the Mesh status before performing a transaction.
	 */
	protected void checkStatus() {
		MeshStatus status = mesh.getStatus();
		switch (status) {
		case READY:
		case STARTING:
			return;
		default:
			throw new RuntimeException("Mesh is not ready. Current status " + status.name() + ". Aborting transaction.");
		}
	}
}
