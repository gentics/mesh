package com.gentics.mesh.plugin.forms.handler.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.gentics.mesh.http.HttpConstants;
import com.gentics.mesh.plugin.PluginContext;
import com.gentics.mesh.plugin.forms.handler.AbstractFormsHandler;
import com.gentics.mesh.plugin.forms.service.StatusService;

import io.reactivex.Completable;
import io.vertx.core.http.HttpHeaders;

public abstract class AbstractNoIndexHandler extends AbstractFormsHandler {

	private final StatusService statusService;
	private final ObjectMapper mapper;

	public AbstractNoIndexHandler(StatusService statusService, ObjectMapper mapper) {
		this.statusService = statusService;
		this.mapper = mapper;
	}

	protected Completable handle(PluginContext context, boolean noIndex) {
		String projectName = context.project().getString("name");
		return statusService.ensureNoIndex(projectName, noIndex).andThen(statusService.check(projectName).doOnEvent((response, t) -> {
			if (response != null) {
				context.response().putHeader(HttpHeaders.CONTENT_TYPE, HttpConstants.APPLICATION_JSON_UTF8)
						.end(mapper.writeValueAsString(response));
			}
		}).ignoreElement());
	}
}
