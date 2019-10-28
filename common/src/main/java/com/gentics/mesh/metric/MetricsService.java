package com.gentics.mesh.metric;

import java.io.IOException;
import java.util.Set;
import java.util.concurrent.atomic.AtomicLong;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.DistributionSummary;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
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
