package com.gentics.mesh.plugin.forms.handler;

import javax.inject.Inject;
import javax.inject.Singleton;

import com.gentics.mesh.plugin.PluginContext;
import com.gentics.mesh.plugin.forms.service.FormService;

import io.reactivex.Completable;
import io.vertx.core.json.JsonObject;

@Singleton
public class UpdateFormHandler extends AbstractFormsHandler {
	private final FormService formService;

	@Inject
	public UpdateFormHandler(FormService formService) {
		this.formService = formService;
	}

	@Override
	public Completable handle(PluginContext context) {
		String projectName = context.project().getString("name");
		String uuid = context.pathParam("formUuid");
		JsonObject formData = context.getBodyAsJson();

		return formService.update(projectName, uuid, formData).doOnEvent((response, t) -> {
			if (response != null) {
				context.response().end(response.toJson());
			}
		}).ignoreElement();
	}
}
