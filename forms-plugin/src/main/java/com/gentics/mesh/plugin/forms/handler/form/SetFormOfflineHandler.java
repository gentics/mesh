package com.gentics.mesh.plugin.forms.handler.form;

import javax.inject.Inject;
import javax.inject.Singleton;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.gentics.mesh.http.HttpConstants;
import com.gentics.mesh.plugin.PluginContext;
import com.gentics.mesh.plugin.common.util.ParameterUtil;
import com.gentics.mesh.plugin.forms.handler.AbstractFormsHandler;
import com.gentics.mesh.plugin.forms.service.FormOnlineStatusService;

import io.reactivex.Completable;
import io.reactivex.Single;
import io.vertx.core.http.HttpHeaders;

/**
 * Handler for setting a form offline
 */
@Singleton
public class SetFormOfflineHandler extends AbstractFormsHandler {
	private final FormOnlineStatusService formOnlineStatusService;

	private final ObjectMapper mapper;

	@Inject
	public SetFormOfflineHandler(FormOnlineStatusService formOnlineStatusService, ObjectMapper mapper) {
		this.formOnlineStatusService = formOnlineStatusService;
		this.mapper = mapper;
	}

	@Override
	public Completable handle(PluginContext context) {
		String projectName = context.project().getString("name");
		Single<String> uuidSingle = ParameterUtil.getUuidFromPath(context, "formUuid");
		Single<String> languageSingle = ParameterUtil.getFromPath(context, "lang", ParameterUtil.LANGUAGE_PATTERN);

		return Single
				.zip(uuidSingle, languageSingle,
						(uuid, language) -> formOnlineStatusService.setOffline(projectName, uuid, language))
				.flatMap(x -> x).doOnEvent((response, t) -> {
					if (response != null) {
						context.response().putHeader(HttpHeaders.CONTENT_TYPE, HttpConstants.APPLICATION_JSON_UTF8)
								.end(mapper.writeValueAsString(response));
					}
				}).ignoreElement();
	}
}
