package com.gentics.mesh.plugin.forms.handler.form;

import javax.inject.Inject;
import javax.inject.Singleton;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.gentics.mesh.http.HttpConstants;
import com.gentics.mesh.plugin.PluginContext;
import com.gentics.mesh.plugin.forms.handler.AbstractFormsHandler;
import com.gentics.mesh.plugin.forms.service.FormOnlineStatusService;

import io.reactivex.Completable;
import io.vertx.core.http.HttpHeaders;

/**
 * Handler for setting a form online
 */
@Singleton
public class SetFormOnlineHandler extends AbstractFormsHandler {
	private final FormOnlineStatusService formOnlineStatusService;

	private final ObjectMapper mapper;

	@Inject
	public SetFormOnlineHandler(FormOnlineStatusService formOnlineStatusService, ObjectMapper mapper) {
		this.formOnlineStatusService = formOnlineStatusService;
		this.mapper = mapper;
	}

	@Override
	public Completable handle(PluginContext context) {
		String projectName = context.project().getString("name");
		String uuid = context.pathParam("formUuid");
		String language = context.pathParam("lang");

		return formOnlineStatusService.setOnline(projectName, uuid, language).doOnEvent((response, t) -> {
			if (response != null) {
				context.response().putHeader(HttpHeaders.CONTENT_TYPE, HttpConstants.APPLICATION_JSON_UTF8)
						.end(mapper.writeValueAsString(response));
			}
		}).ignoreElement();
	}
}
