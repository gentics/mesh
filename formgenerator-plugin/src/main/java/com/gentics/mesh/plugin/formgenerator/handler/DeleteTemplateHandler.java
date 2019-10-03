package com.gentics.mesh.plugin.formgenerator.handler;

import javax.inject.Inject;
import javax.inject.Singleton;

import com.gentics.mesh.plugin.PluginContext;
import com.gentics.mesh.plugin.formgenerator.service.TemplateService;

import io.reactivex.Completable;

@Singleton
public class DeleteTemplateHandler extends AbstractFormGeneratorHandler {

	private final TemplateService templateService;

	@Inject
	public DeleteTemplateHandler(TemplateService templateService) {
		this.templateService = templateService;
	}

	@Override
	public Completable handle(PluginContext context) {
		String projectName = context.project().getString("name");
		String uuid = context.pathParam("templateUuid");
		return templateService.delete(projectName, uuid).doOnEvent(t -> {
			if (t == null) {
				context.response().setStatusCode(204).end();
			}
		});
	}
}
