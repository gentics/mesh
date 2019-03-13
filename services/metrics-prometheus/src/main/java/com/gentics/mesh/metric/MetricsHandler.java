package com.gentics.mesh.metric;

import static io.netty.handler.codec.http.HttpResponseStatus.OK;

import java.io.IOException;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.inject.Inject;

import com.gentics.mesh.context.InternalActionContext;

import io.prometheus.client.exporter.common.TextFormat;
import io.vertx.core.buffer.Buffer;

public class MetricsHandler {

	private MetricsService service;

	@Inject
	public MetricsHandler(MetricsService service) {
		this.service = service;
	}

	public void handleMetrics(InternalActionContext ac) {
		try {
			Set<String> params = parse(ac);
			Buffer buffer = service.toPrometheusFormat(params);
			ac.send(buffer.toString(), OK, TextFormat.CONTENT_TYPE_004);
		} catch (IOException e) {
			ac.fail(e);
		}
	}

	private Set<String> parse(InternalActionContext ac) {
		List<String> names = ac.getParameters().getAll("name[]");
		return new HashSet<>(names);
	}
}
