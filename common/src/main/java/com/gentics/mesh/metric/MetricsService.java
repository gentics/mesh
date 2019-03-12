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
	 * Return the drop wizard registry.
	 * 
	 * @return
	 */
	MetricRegistry getMetricRegistry();

	default Meter meter(Metrics metric) {
		return getMetricRegistry().meter(metric.key());
	}

	default Timer timer(Metrics metric) {
		return getMetricRegistry().timer(metric.key());
	}

	default Counter counter(Metrics metric) {
		return getMetricRegistry().counter(metric.key());
	}

	default ResettableCounter resetableCounter(Metrics metric) {
		return (ResettableCounter) getMetricRegistry().counter(metric.key(), () -> new ResettableCounter());
	}

}
