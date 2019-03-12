package com.gentics.mesh.metric;

import java.io.IOException;
import java.io.Writer;
import java.util.Set;

import javax.inject.Inject;
import javax.inject.Singleton;

import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.SharedMetricRegistries;

import io.prometheus.client.CollectorRegistry;
import io.prometheus.client.dropwizard.DropwizardExports;
import io.prometheus.client.exporter.common.TextFormat;
import io.vertx.core.buffer.Buffer;

@Singleton
public class DropwizardMetricsService implements MetricsService {

	private final CollectorRegistry registry;

	private final MetricRegistry metricRegistry;

	@Inject
	public DropwizardMetricsService() {
		this.registry = CollectorRegistry.defaultRegistry;
		this.metricRegistry = setupRegistry();
	}

	private MetricRegistry setupRegistry() {
		MetricRegistry metricRegistry = SharedMetricRegistries.getOrCreate("mesh");
		registry.register(new DropwizardExports(metricRegistry));
		return metricRegistry;
	}

	
	@Override
	public MetricRegistry getMetricRegistry() {
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
