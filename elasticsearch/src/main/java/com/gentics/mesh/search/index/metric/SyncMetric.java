package com.gentics.mesh.search.index.metric;

import java.util.HashMap;
import java.util.Map;
import java.util.SortedMap;

import com.codahale.metrics.Counter;
import com.codahale.metrics.Meter;
import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.SharedMetricRegistries;

import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;

/**
 * Container for index sync metric counters.
 */
public class SyncMetric {

	private static final Logger log = LoggerFactory.getLogger(SyncMetric.class);

	private static final MetricRegistry metricRegistry = SharedMetricRegistries.getOrCreate("mesh");

	private Counter insertTotal;
	private Counter deleteTotal;
	private Counter updateTotal;

	private Meter insertMeter;
	private Meter deleteMeter;
	private Meter updateMeter;

	private Counter insertCount;
	private Counter deleteCount;
	private Counter updateCount;

	/**
	 * Create a new metric object and reset all managed metrics for the given type.
	 * 
	 * @param type
	 */
	public SyncMetric(String type) {
		insertTotal = metricRegistry.counter("index.sync." + type + ".insert.total");
		deleteTotal = metricRegistry.counter("index.sync." + type + ".delete.total");
		updateTotal = metricRegistry.counter("index.sync." + type + ".update.total");

		insertMeter = metricRegistry.meter("index.sync." + type + ".insert.meter");
		deleteMeter = metricRegistry.meter("index.sync." + type + ".delete.meter");
		updateMeter = metricRegistry.meter("index.sync." + type + ".update.meter");

		insertCount = metricRegistry.counter("index.sync." + type + ".insert.pending");
		deleteCount = metricRegistry.counter("index.sync." + type + ".delete.pending");
		updateCount = metricRegistry.counter("index.sync." + type + ".update.pending");
	}

	/**
	 * Helper method which will load a snapshot of all metrics and return a map which lists them.
	 * 
	 * @param type
	 * @return
	 */
	public static Map<String, Object> fetch(String type) {
		Map<String, Object> map = new HashMap<>();
		long insertTotal = getCount("index.sync." + type + ".insert.total", 0);
		long deleteTotal = getCount("index.sync." + type + ".delete.total", 0);
		long updateTotal = getCount("index.sync." + type + ".update.total", 0);

		map.put("insert.total", insertTotal);
		map.put("delete.total", deleteTotal);
		map.put("update.total", updateTotal);

		long insertPending = getCount("index.sync." + type + ".insert.pending", 0);
		long deletePending = getCount("index.sync." + type + ".delete.pending", 0);
		long updatePending = getCount("index.sync." + type + ".update.pending", 0);

		map.put("insert.pending", insertPending);
		map.put("delete.pending", deletePending);
		map.put("update.pending", updatePending);
		return map;
	}

	private static long getCount(String name, long fallback) {
		MetricRegistry registry = SharedMetricRegistries.getOrCreate("mesh");
		SortedMap<String, Counter> counters = registry.getCounters();
		Counter counter = counters.get(name);
		if (counter == null) {
			return fallback;
		} else {
			return counter.getCount();
		}
	}

	/**
	 * Removes all sync metrics.
	 */
	public static void reset() {
		if (log.isDebugEnabled()) {
			log.debug("Resetting all index.sync metrics by removing them.");
		}
		metricRegistry.removeMatching((name, metric) -> {
			return name.startsWith("index.sync");
		});
	}

	public void incInsert(int size) {
		insertTotal.inc(size);
		insertCount.inc(size);
	}

	public void decInsert() {
		insertMeter.mark();
		insertCount.dec();
	}

	public void incDelete(int size) {
		deleteTotal.inc(size);
		deleteCount.inc(size);
	}

	public void decDelete() {
		deleteMeter.mark();
		deleteCount.dec();
	}

	public void incUpdate(int size) {
		updateTotal.inc(size);
		updateCount.inc(size);
	}

	public void decUpdate() {
		updateMeter.mark();
		updateCount.dec();
	}

}
