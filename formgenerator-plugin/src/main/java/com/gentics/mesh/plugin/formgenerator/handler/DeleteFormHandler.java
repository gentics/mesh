package com.gentics.mesh.plugin.formgenerator.handler;

import javax.inject.Inject;
import javax.inject.Singleton;

import com.gentics.mesh.plugin.PluginContext;
import com.gentics.mesh.plugin.formgenerator.service.FormService;

import io.reactivex.Completable;

@Singleton
public class DeleteFormHandler extends AbstractFormGeneratorHandler {

	private final FormService formService;

	@Inject
	public DeleteFormHandler(FormService formService) {
		this.formService = formService;
	}

	@Override
	public Completable handle(PluginContext context) {
		String uuid = context.pathParam("formUuid");
		return formService.delete(context.project().getString("name"), uuid).doOnEvent((response, t) -> {
			if (t == null) {
				context.response().setStatusCode(204).end();
			}
		}).ignoreElement();
	}
}
