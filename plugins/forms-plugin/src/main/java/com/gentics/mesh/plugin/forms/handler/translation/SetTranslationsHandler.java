package com.gentics.mesh.plugin.forms.handler.translation;

import java.io.IOException;

import javax.inject.Inject;
import javax.inject.Singleton;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.gentics.mesh.http.HttpConstants;
import com.gentics.mesh.plugin.PluginContext;
import com.gentics.mesh.plugin.forms.handler.AbstractFormsHandler;
import com.gentics.mesh.plugin.forms.model.TranslationsModel;
import com.gentics.mesh.plugin.forms.service.TranslationsService;

import io.reactivex.Completable;
import io.vertx.core.http.HttpHeaders;

/**
 * Handler for saving translations of the FormsPlugin
 */
@Singleton
public class SetTranslationsHandler extends AbstractFormsHandler {

	private final TranslationsService translationService;
	private final ObjectMapper mapper;

	@Inject
	public SetTranslationsHandler(TranslationsService translationService, ObjectMapper mapper) {
		this.translationService = translationService;
		this.mapper = mapper;
	}

	@Override
	public Completable handle(PluginContext context) {
		String projectName = context.project().getString("name");
		TranslationsModel model;
		try {
			model = mapper.readValue(context.body().asString(), TranslationsModel.class);
		} catch (IOException e) {
			return Completable.error(e);
		}

		return translationService.setTranslations(projectName, model).doOnEvent((response, t) -> {
			if (response != null) {
				context.response()
						.putHeader(HttpHeaders.CONTENT_TYPE, HttpConstants.APPLICATION_JSON_UTF8)
						.end(mapper.writeValueAsString(response));
			}
		}).ignoreElement();
	}
}
