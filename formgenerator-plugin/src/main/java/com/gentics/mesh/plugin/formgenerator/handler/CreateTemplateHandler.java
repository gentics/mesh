package com.gentics.mesh.plugin.formgenerator.handler;

import javax.inject.Inject;
import javax.inject.Singleton;

import com.gentics.mesh.plugin.PluginContext;
import com.gentics.mesh.plugin.formgenerator.service.TemplateService;

import io.reactivex.Completable;
import io.vertx.core.json.JsonObject;

@Singleton
public class CreateTemplateHandler extends AbstractFormGeneratorHandler {

	private final TemplateService templateService;

	@Inject
	public CreateTemplateHandler(TemplateService templateService) {
		this.templateService = templateService;
	}

	@Override
	public Completable handle(PluginContext context) {
		String projectName = context.project().getString("name");
		JsonObject formData = context.getBodyAsJson();
		return templateService.create(projectName, formData).doOnEvent((response, t) -> {
			if (response != null) {
				context.response().end(response.encodePrettily());
			}
		}).ignoreElement();
	}
}
