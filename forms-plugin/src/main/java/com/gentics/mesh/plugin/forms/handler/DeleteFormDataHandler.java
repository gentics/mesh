package com.gentics.mesh.plugin.forms.handler;

import javax.inject.Inject;
import javax.inject.Singleton;

import com.gentics.mesh.plugin.PluginContext;
import com.gentics.mesh.plugin.forms.service.FormDataService;

import io.reactivex.Completable;

@Singleton
public class DeleteFormDataHandler extends AbstractFormsHandler {

	private final FormDataService formDataService;

	@Inject
	public DeleteFormDataHandler(FormDataService formDataService) {
		this.formDataService = formDataService;
	}

	@Override
	public Completable handle(PluginContext context) {
		String projectName = context.project().getString("name");
		String formUuid = context.pathParam("formUuid");
		String formDataUuid = context.pathParam("formDataUuid");

		return formDataService.delete(projectName, formUuid, formDataUuid).doOnEvent((response, t) -> {
			if (t == null) {
				context.response().setStatusCode(204).end();
			}
		}).ignoreElement();
	}
}
