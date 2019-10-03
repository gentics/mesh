package com.gentics.mesh.plugin.formgenerator.handler;

import javax.inject.Inject;
import javax.inject.Singleton;

import com.gentics.mesh.plugin.PluginContext;
import com.gentics.mesh.plugin.formgenerator.service.TemplateService;

import io.reactivex.Completable;

@Singleton
public class ListTemplatesHandler extends AbstractFormGeneratorHandler {
	private final TemplateService templateService;

	@Inject
	public ListTemplatesHandler(TemplateService templateService) {
		this.templateService = templateService;
	}

	@Override
	public Completable handle(PluginContext context) {
		String projectName = context.project().getString("name");

		return templateService.list(projectName).doOnEvent((response, t) -> {
			if (response != null) {
				context.response().end(response.encodePrettily());
			}
		}).ignoreElement();
	}
}
