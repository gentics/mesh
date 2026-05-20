
package com.gentics.mesh.plugin.forms.handler;

import javax.inject.Inject;
import javax.inject.Singleton;

import com.gentics.mesh.json.JsonUtil;
import com.gentics.mesh.plugin.PluginContext;
import com.gentics.mesh.plugin.common.util.ParameterUtil;
import com.gentics.mesh.plugin.forms.context.FormDataCreateContext;
import com.gentics.mesh.plugin.forms.context.FormDataUpdateContext;
import com.gentics.mesh.plugin.forms.service.FormDataService;

import io.reactivex.Completable;
import io.vertx.core.http.HttpServerResponse;

@Singleton
public class SubmitDataHandler extends AbstractFormsHandler {
	private final FormDataService formDataService;

	@Inject
	public SubmitDataHandler(FormDataService formDataService) {
		this.formDataService = formDataService;
	}

	@Override
	public Completable handle(PluginContext context) {
		String formDataUuid = ParameterUtil.getQueryParam(context, "formDataUuid");
		return (formDataUuid == null
				? FormDataCreateContext.getCreate(context, true, true).flatMap(saveContext -> formDataService.create(saveContext))
				: FormDataUpdateContext.getUpdate(context, true, true, formDataUuid).flatMap(saveContext -> formDataService.update(saveContext)))
			.doOnSuccess(response -> {
				HttpServerResponse serverResponse = context.response();
				serverResponse.end(JsonUtil.toJson(response));
			})
			.ignoreElement();
	}
}
