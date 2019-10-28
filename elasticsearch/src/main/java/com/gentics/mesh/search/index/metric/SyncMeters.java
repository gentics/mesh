package com.gentics.mesh.search.index.metric;

import static com.gentics.mesh.metric.SyncMetric.Operation.DELETE;
import static com.gentics.mesh.metric.SyncMetric.Operation.INSERT;
import static com.gentics.mesh.metric.SyncMetric.Operation.UPDATE;

import java.util.HashMap;
import java.util.Map;

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

		snapshot.put("insert.total", insert.getTotalSynced());
		snapshot.put("update.total", update.getTotalSynced());
		snapshot.put("delete.total", delete.getTotalSynced());

		snapshot.put("insert.pending", insert.getCurrentPending());
		snapshot.put("update.pending", update.getCurrentPending());
		snapshot.put("delete.pending", delete.getCurrentPending());

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
