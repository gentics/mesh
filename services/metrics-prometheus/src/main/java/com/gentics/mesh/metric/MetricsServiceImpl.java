package com.gentics.mesh.metric;

import javax.inject.Inject;
import javax.inject.Singleton;

import com.gentics.mesh.etc.config.MeshOptions;
import com.gentics.mesh.etc.config.MonitoringConfig;

import io.micrometer.core.instrument.MeterRegistry;
import io.prometheus.client.CollectorRegistry;

@Singleton
public class MetricsServiceImpl implements MetricsService {

	private final CollectorRegistry registry;

	private final MeterRegistry metricRegistry;

	private MonitoringConfig options;

	@Inject
	public MetricsServiceImpl(MeshOptions options, MeterRegistry meterRegistry) {
		this.options = options.getMonitoringOptions();
		this.registry = CollectorRegistry.defaultRegistry;
		this.metricRegistry = meterRegistry;
	}

	@Override
	public MeterRegistry getMetricRegistry() {
		return metricRegistry;
	}

	public CollectorRegistry getRegistry() {
		return registry;
	}

	@Override
	public boolean isEnabled() {
		return options != null && options.isEnabled();
	}

}
