package com.gentics.mesh.plugin.formgenerator.handler;

import javax.inject.Inject;
import javax.inject.Singleton;

import com.gentics.mesh.plugin.PluginContext;
import com.gentics.mesh.plugin.formgenerator.service.FormDataService;

import io.reactivex.Completable;

@Singleton
public class ExportHandler extends AbstractFormGeneratorHandler {
	private final FormDataService formDataService;

	@Inject
	public ExportHandler(FormDataService formDataService) {
		this.formDataService = formDataService;
	}

	@Override
	public Completable handle(PluginContext context) {
		String projectName = context.project().getString("name");
		String formUuid = context.pathParam("formUuid");

		return formDataService.export(projectName, formUuid, context).doOnEvent((exportData, t) -> {
			if (exportData != null) {
				context.response().end(exportData);
			}
		}).ignoreElement();
	}
}
