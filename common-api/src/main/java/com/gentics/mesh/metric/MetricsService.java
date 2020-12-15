package com.gentics.mesh.metric;

import java.util.concurrent.atomic.AtomicLong;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.DistributionSummary;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;

/**
 * Service which provides metric objects to collect metric data.
 */
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

	/**
	 * Return a micrometer meter for the key of the given metric.
	 * 
	 * @param metric
	 * @return
	 */
	default DistributionSummary meter(Metric metric) {
		return getMetricRegistry().summary(metric.key());
	}

	/**
	 * Return a micrometer timer for the key of the given metric.
	 * 
	 * @param metric
	 * @return
	 */
	default Timer timer(Metric metric) {
		return getMetricRegistry().timer(metric.key());
	}

	/**
	 * Return a micrometer counter for the key of the given metric.
	 * 
	 * @param metric
	 * @return
	 */
	default Counter counter(Metric metric) {
		return getMetricRegistry().counter(metric.key());
	}

	/**
	 * Return a micrometer long gauge for the key of the given metric.
	 * 
	 * @param metric
	 * @return
	 */
	default AtomicLong longGauge(Metric metric) {
		return getMetricRegistry().gauge(metric.key(), new AtomicLong(0));
	}
}
