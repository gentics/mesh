package com.gentics.mesh.metric;

import java.io.IOException;
import java.util.Set;

import com.codahale.metrics.Counter;
import com.codahale.metrics.Meter;
import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.Timer;

import io.vertx.core.buffer.Buffer;

public interface MetricsService {

	/**
	 * Transform the metrics to prometheus output text.
	 * 
	 * @param params
	 * @return
	 * @throws IOException
	 */
	Buffer toPrometheusFormat(Set<String> params) throws IOException;

	/**
	 * Check whether the metrics system is enabled.
	 * 
	 * @return
	 */
	boolean isEnabled();

	/**
	 * Return the drop wizard registry.
	 * 
	 * @return
	 */
	MetricRegistry getMetricRegistry();

	default Meter meter(Metric metric) {
		return getMetricRegistry().meter(metric.key());
	}

	default Timer timer(Metric metric) {
		return getMetricRegistry().timer(metric.key());
	}

	default Counter counter(Metric metric) {
		return getMetricRegistry().counter(metric.key());
	}

	default ResettableCounter resettableCounter(Metric metric) {
		return (ResettableCounter) getMetricRegistry().counter(metric.key(), () -> new ResettableCounter());
	}

}
