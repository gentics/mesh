package com.gentics.mesh.plugin.forms.handler;

import javax.inject.Inject;
import javax.inject.Singleton;

import com.gentics.mesh.json.JsonUtil;
import com.gentics.mesh.plugin.PluginContext;
import com.gentics.mesh.plugin.forms.context.FormDataUpdateContext;
import com.gentics.mesh.plugin.forms.service.FormDataService;

import io.reactivex.Completable;

@Singleton
public class UpdatePostedJsonDataHandler extends AbstractFormsHandler {
	private final FormDataService formDataService;

	@Inject
	public UpdatePostedJsonDataHandler(FormDataService formDataService) {
		this.formDataService = formDataService;
	}

	@Override
	public Completable handle(PluginContext context) {
		return FormDataUpdateContext.getUpdate(context, false).flatMap(saveContext -> formDataService.update(saveContext))
				.doOnSuccess(response -> {
					context.response().end(JsonUtil.toJson(response));
				}).ignoreElement();
	}
}
