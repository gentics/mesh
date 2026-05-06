package com.gentics.mesh.plugin.forms.handler.translation;

import javax.inject.Inject;
import javax.inject.Singleton;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.gentics.mesh.http.HttpConstants;
import com.gentics.mesh.plugin.PluginContext;
import com.gentics.mesh.plugin.forms.handler.AbstractFormsHandler;
import com.gentics.mesh.plugin.forms.service.TranslationsService;

import io.reactivex.Completable;
import io.vertx.core.http.HttpHeaders;

/**
 * Handler for loading translations for a specific form and language
 */
@Singleton
public class GetTranslationsHandler extends AbstractFormsHandler {

	private final TranslationsService translationService;
	private final ObjectMapper mapper;

	@Inject
	public GetTranslationsHandler(TranslationsService translationService, ObjectMapper mapper) {
		this.translationService = translationService;
		this.mapper = mapper;
	}

	@Override
	public Completable handle(PluginContext context) {
		String projectName = context.project().getString("name");
		String formUuid = context.pathParam("formUuid");
		String lang = context.pathParam("lang");

		return translationService.getTranslations(projectName, formUuid, lang).doOnEvent((response, t) -> {
			if (response != null) {
				context.response()
						.putHeader(HttpHeaders.CONTENT_TYPE, HttpConstants.APPLICATION_JSON_UTF8)
						.end(mapper.writeValueAsString(response));
				return;
			}
			
			context.response()
				.putHeader(HttpHeaders.CONTENT_TYPE, HttpConstants.APPLICATION_JSON_UTF8)
				.setStatusCode(500)
				.end();
		}).ignoreElement();
	}
}
