package com.gentics.mesh.search.index.metric;

import static com.gentics.mesh.metric.SyncMetric.Meter.PENDING;
import static com.gentics.mesh.metric.SyncMetric.Meter.TOTAL;
import static com.gentics.mesh.metric.SyncMetric.Operation.DELETE;
import static com.gentics.mesh.metric.SyncMetric.Operation.INSERT;
import static com.gentics.mesh.metric.SyncMetric.Operation.UPDATE;

import java.util.HashMap;
import java.util.Map;

import com.gentics.mesh.metric.MetricsService;
import com.gentics.mesh.metric.SyncMetric;

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
	private final String type;

	/**
	 * Create a new metric object and reset all managed metrics for the given type.
	 *
	 * @param metrics
	 * @param type
	 */
	public SyncMeters(MetricsService metrics, String type) {
		this.type = type;
		insert = new SyncMeter(metrics, type, INSERT);
		update = new SyncMeter(metrics, type, UPDATE);
		delete = new SyncMeter(metrics, type, DELETE);
	}

	public Map<String, Object> createSnapshot() {
		Map<String, Object> snapshot = new HashMap<>(6);

		snapshot.put(new SyncMetric(type, INSERT, TOTAL).key(), insert.getTotalSynced());
		snapshot.put(new SyncMetric(type, UPDATE, TOTAL).key(), update.getTotalSynced());
		snapshot.put(new SyncMetric(type, DELETE, TOTAL).key(), delete.getTotalSynced());

		snapshot.put(new SyncMetric(type, INSERT, PENDING).key(), insert.getCurrentPending());
		snapshot.put(new SyncMetric(type, UPDATE, PENDING).key(), update.getCurrentPending());
		snapshot.put(new SyncMetric(type, DELETE, PENDING).key(), delete.getCurrentPending());

		return snapshot;
	}

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
