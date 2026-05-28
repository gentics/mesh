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

import com.gentics.mesh.plugin.forms.service.TranslationsService;
import io.reactivex.Completable;
import io.vertx.core.http.HttpServerResponse;

@Singleton
public class SaveDataHandler extends AbstractFormsHandler {
	private final FormDataService formDataService;
	private final TranslationsService translationService;

	@Inject
	public SaveDataHandler(FormDataService formDataService, TranslationsService translationsService) {
		this.formDataService = formDataService;
		this.translationService = translationsService;
	}

	@Override
	public Completable handle(PluginContext context) {
		String formDataUuid = ParameterUtil.getQueryParam(context, "formDataUuid");
		return (formDataUuid == null
				? FormDataCreateContext.getCreate(context, true, false, translationService).flatMap(formDataService::create)
				: FormDataUpdateContext.getUpdate(context, true, false, formDataUuid, translationService).flatMap(formDataService::update))
			.doOnSuccess(response -> {
				HttpServerResponse serverResponse = context.response();
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
