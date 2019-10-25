package com.gentics.mesh.metric;

import java.io.IOException;
import java.io.Writer;
import java.util.Set;

import javax.inject.Inject;
import javax.inject.Singleton;

import com.gentics.mesh.etc.config.MeshOptions;
import com.gentics.mesh.etc.config.MonitoringConfig;

import io.micrometer.core.instrument.MeterRegistry;
import io.prometheus.client.CollectorRegistry;
import io.prometheus.client.exporter.common.TextFormat;
import io.vertx.core.buffer.Buffer;

@Singleton
public class DropwizardMetricsService implements MetricsService {

	private final CollectorRegistry registry;

	private final MeterRegistry metricRegistry;

	private MonitoringConfig options;

	@Inject
	public DropwizardMetricsService(MeshOptions options, MeterRegistry meterRegistry) {
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
	public Buffer toPrometheusFormat(Set<String> params) throws IOException {
		final BufferWriter writer = new BufferWriter();
		TextFormat.write004(writer, getRegistry().filteredMetricFamilySamples(params));
		return writer.getBuffer();
	}

	@Override
	public boolean isEnabled() {
		return options != null && options.isEnabled();
	}

	/**
	 * Wrap a Vert.x Buffer as a Writer so it can be used with TextFormat writer
	 */
	private static class BufferWriter extends Writer {

		private final Buffer buffer = Buffer.buffer();

		@Override
		public void write(char[] cbuf, int off, int len) throws IOException {
			buffer.appendString(new String(cbuf, off, len));
		}

		@Override
		public void flush() throws IOException {
			// NO-OP
		}

		@Override
		public void close() throws IOException {
			// NO-OP
		}

		Buffer getBuffer() {
			return buffer;
		}
	}

}
