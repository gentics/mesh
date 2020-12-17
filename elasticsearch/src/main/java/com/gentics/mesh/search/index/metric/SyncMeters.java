package com.gentics.mesh.search.index.metric;

import static com.gentics.mesh.search.index.metric.SyncMetric.Operation.DELETE;
import static com.gentics.mesh.search.index.metric.SyncMetric.Operation.INSERT;
import static com.gentics.mesh.search.index.metric.SyncMetric.Operation.UPDATE;

import com.gentics.mesh.core.rest.search.EntityMetrics;
import com.gentics.mesh.metric.MetricsService;

import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;

/**
 * Container for index sync metric counters.
 */
public class SyncMeters {

	private static final Logger log = LoggerFactory.getLogger(SyncMeters.class);

	private final SyncMeter insert;
	private final SyncMeter delete;
	private final SyncMeter update;

	/**
	 * Create a new metric object and reset all managed metrics for the given type.
	 *
	 * @param metrics
	 * @param type
	 */
	public SyncMeters(MetricsService metrics, String type) {
		insert = new SyncMeter(metrics, type, INSERT);
		update = new SyncMeter(metrics, type, UPDATE);
		delete = new SyncMeter(metrics, type, DELETE);
	}

	public EntityMetrics createSnapshot() {
		return new EntityMetrics()
			.setInsert(insert.createSnapshot())
			.setUpdate(update.createSnapshot())
			.setDelete(delete.createSnapshot());
	}

	/**
	 * Reset all counter.
	 */
	public void reset() {
		insert.reset();
		update.reset();
		delete.reset();
	}

	public SyncMeter getInsertMeter() {
		return insert;
	}

	public SyncMeter getDeleteMeter() {
		return delete;
	}

	public SyncMeter getUpdateMeter() {
		return update;
	}

}
