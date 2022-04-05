package com.gentics.mesh.plugin.forms.handler.form;

import javax.inject.Inject;
import javax.inject.Singleton;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.gentics.mesh.http.HttpConstants;
import com.gentics.mesh.plugin.PluginContext;
import com.gentics.mesh.plugin.forms.handler.AbstractFormsHandler;
import com.gentics.mesh.plugin.forms.service.FormService;

import io.reactivex.Completable;
import io.vertx.core.http.HttpHeaders;
import io.vertx.core.json.JsonObject;

@Singleton
public class RenderFormPreviewAsJsonHandler extends AbstractFormsHandler {
	private final FormService formService;

	private final ObjectMapper mapper;

	@Inject
	public RenderFormPreviewAsJsonHandler(FormService formService, ObjectMapper mapper) {
		this.formService = formService;
		this.mapper = mapper;
	}

	@Override
	public Completable handle(PluginContext context) {
		String projectName = context.project().getString("name");
		String uuid = context.pathParam("formUuid");
		JsonObject formData = context.getBodyAsJson();

		return formService.renderPreviewAsJson(projectName, uuid, formData, context).doOnEvent((response, t) -> {
				if (response != null) {
					context.response().putHeader(HttpHeaders.CONTENT_TYPE, HttpConstants.APPLICATION_JSON_UTF8)
					.end(mapper.writeValueAsString(response));
				}

		}).ignoreElement();
	}
}
