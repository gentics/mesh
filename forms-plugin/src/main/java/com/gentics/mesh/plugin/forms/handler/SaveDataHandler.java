package com.gentics.mesh.plugin.forms.handler;

import javax.inject.Inject;
import javax.inject.Singleton;

import com.gentics.mesh.json.JsonUtil;
import com.gentics.mesh.plugin.PluginContext;
import com.gentics.mesh.plugin.common.util.ParameterUtil;
import com.gentics.mesh.plugin.forms.context.FormDataCreateContext;
import com.gentics.mesh.plugin.forms.context.FormDataUpdateContext;
import com.gentics.mesh.plugin.forms.model.CaptchaValidationException;
import com.gentics.mesh.plugin.forms.service.FormDataService;

import io.reactivex.Completable;
import io.vertx.core.http.HttpServerResponse;

@Singleton
public class SaveDataHandler extends AbstractFormsHandler {
	private final FormDataService formDataService;

	@Inject
	public SaveDataHandler(FormDataService formDataService) {
		this.formDataService = formDataService;
	}

	@Override
	public Completable handle(PluginContext context) {
		String formDataUuid = ParameterUtil.getQueryParam(context, "formDataUuid");
		return (formDataUuid == null
				? FormDataCreateContext.getCreate(context, true, false).flatMap(saveContext -> formDataService.create(saveContext))
				: FormDataUpdateContext.getUpdate(context, true, false, formDataUuid).flatMap(saveContext -> formDataService.update(saveContext)))
			.doOnSuccess(response -> {
				String redirectUrl = response.getRedirectUrl();
				HttpServerResponse serverResponse = context.response();
				if (redirectUrl != null && !"".equals(redirectUrl)) {
					serverResponse = serverResponse.setStatusCode(303).putHeader("Location", redirectUrl);
				} else {
					serverResponse = serverResponse.setStatusCode(200);
				}
				serverResponse.end(JsonUtil.toJson(response));
			})
			.ignoreElement()
			.onErrorResumeNext(e -> {
				if (e instanceof CaptchaValidationException captchaEx) {
					context.response()
						.setStatusCode(400)
						.putHeader("content-type", "application/json")
						.end(captchaEx.toResponseBody());
					return Completable.complete();
				}
				return Completable.error(e);
			});
	}
}
