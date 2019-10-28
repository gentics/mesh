package com.gentics.mesh.metric;

import java.util.concurrent.atomic.AtomicLong;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.DistributionSummary;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;

public interface MetricsService {

	/**
	 * Check whether the metrics system is enabled.
	 * 
	 * @return
	 */
	boolean isEnabled();

	/**
	 * Return the metric registry.
	 * 
	 * @return
	 */
	MeterRegistry getMetricRegistry();

	default DistributionSummary meter(Metric metric) {
		return getMetricRegistry().summary(metric.key());
	}

	default Timer timer(Metric metric) {
		return getMetricRegistry().timer(metric.key());
	}

	default Counter counter(Metric metric) {
		return getMetricRegistry().counter(metric.key());
	}

	default AtomicLong longGauge(Metric metric) {
		return getMetricRegistry().gauge(metric.key(), new AtomicLong(0));
	}
}
