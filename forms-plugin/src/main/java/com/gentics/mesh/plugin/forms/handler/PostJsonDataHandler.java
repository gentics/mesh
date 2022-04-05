package com.gentics.mesh.plugin.forms.handler;

import javax.inject.Inject;
import javax.inject.Singleton;

import com.gentics.mesh.json.JsonUtil;
import com.gentics.mesh.plugin.PluginContext;
import com.gentics.mesh.plugin.forms.context.FormDataSaveContext;
import com.gentics.mesh.plugin.forms.service.FormDataService;

import io.reactivex.Completable;

@Singleton
public class PostJsonDataHandler extends AbstractFormsHandler {
	private final FormDataService formDataService;

	@Inject
	public PostJsonDataHandler(FormDataService formDataService) {
		this.formDataService = formDataService;
	}

	@Override
	public Completable handle(PluginContext context) {
		return formDataService.create(new FormDataSaveContext(context, false)).doOnSuccess(response -> {
			context.response().end(JsonUtil.toJson(response));
		}).ignoreElement();
	}
}
