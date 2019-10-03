package com.gentics.mesh.plugin.formgenerator.handler;

import javax.inject.Inject;
import javax.inject.Singleton;

import com.gentics.mesh.plugin.PluginContext;
import com.gentics.mesh.plugin.formgenerator.service.FormService;

import io.reactivex.Completable;

@Singleton
public class LoadFormSchemaHandler extends AbstractFormGeneratorHandler {
	private final FormService formService;

	@Inject
	public LoadFormSchemaHandler(FormService formService) {
		this.formService = formService;
	}

	@Override
	public Completable handle(PluginContext context) {
		String uuid = context.pathParam("formUuid");
		return formService.getFormSchemaWithPageId(context.project().getString("name"), uuid).doOnEvent((response, t) -> {
			if (response != null) {
				context.response().end(response.encodePrettily());
			}
		}).ignoreElement();
	}
}
