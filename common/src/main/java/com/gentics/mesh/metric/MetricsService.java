package com.gentics.mesh.metric;

import java.io.IOException;
import java.util.Set;

import com.codahale.metrics.MetricRegistry;

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

}
