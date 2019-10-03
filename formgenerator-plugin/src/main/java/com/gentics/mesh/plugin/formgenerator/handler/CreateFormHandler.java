package com.gentics.mesh.plugin.formgenerator.handler;

import javax.inject.Inject;
import javax.inject.Singleton;

import com.gentics.mesh.plugin.PluginContext;
import com.gentics.mesh.plugin.formgenerator.service.FormService;

import io.reactivex.Completable;
import io.vertx.core.json.JsonObject;

@Singleton
public class CreateFormHandler extends AbstractFormGeneratorHandler {

	private final FormService formService;

	@Inject
	public CreateFormHandler(FormService formService) {
		this.formService = formService;
	}

	@Override
	public Completable handle(PluginContext context) {
		String projectName = context.project().getString("name");
		JsonObject formData = context.getBodyAsJson();
		return formService.create(projectName, formData).doOnEvent((response, t) -> {
			if (response != null) {
				context.response().end(response.toJson());
			}
		}).ignoreElement();
	}
}
