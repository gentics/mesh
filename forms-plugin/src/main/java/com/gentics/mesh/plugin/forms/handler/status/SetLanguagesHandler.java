package com.gentics.mesh.plugin.forms.handler.status;

import java.io.IOException;

import javax.inject.Inject;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.gentics.mesh.http.HttpConstants;
import com.gentics.mesh.plugin.PluginContext;
import com.gentics.mesh.plugin.forms.handler.AbstractFormsHandler;
import com.gentics.mesh.plugin.forms.model.LanguagesModel;
import com.gentics.mesh.plugin.forms.service.StatusService;

import io.reactivex.Completable;
import io.vertx.core.http.HttpHeaders;

/**
 * Handler for setting the languages to the FormsPlugin
 */
public class SetLanguagesHandler extends AbstractFormsHandler {
	private final StatusService statusService;

	private final ObjectMapper mapper;

	@Inject
	public SetLanguagesHandler(StatusService statusService, ObjectMapper mapper) {
		this.statusService = statusService;
		this.mapper = mapper;
	}

	@Override
	public Completable handle(PluginContext context) {
		String projectName = context.project().getString("name");
		LanguagesModel languagesModel;
		try {
			languagesModel = mapper.readValue(context.getBodyAsString(), LanguagesModel.class);
		} catch (IOException e) {
			return Completable.error(e);
		}

		return statusService.setLanguages(projectName, languagesModel.getLanguages()).doOnEvent((response, t) -> {
			if (response != null) {
				context.response().putHeader(HttpHeaders.CONTENT_TYPE, HttpConstants.APPLICATION_JSON_UTF8)
						.end(mapper.writeValueAsString(response));
			}
		}).ignoreElement();
	}
}
