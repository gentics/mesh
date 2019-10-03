package com.gentics.mesh.plugin.formgenerator.handler;

import javax.inject.Inject;
import javax.inject.Singleton;

import com.gentics.mesh.plugin.PluginContext;
import com.gentics.mesh.plugin.formgenerator.service.TemplateService;

import io.reactivex.Completable;

@Singleton
public class LoadTemplateHandler extends AbstractFormGeneratorHandler {

	private final TemplateService templateService;

	@Inject
	public LoadTemplateHandler(TemplateService templateService) {
		this.templateService = templateService;
	}

	@Override
	public Completable handle(PluginContext context) {
		String projectName = context.project().getString("name");
		String uuid = context.pathParam("templateUuid");

		return templateService.load(projectName, uuid).doOnEvent((response, t) -> {
			if (response != null) {
				context.response().end(response.encodePrettily());
			}
		}).ignoreElement();
	}
}
