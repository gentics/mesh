
package com.gentics.mesh.plugin.forms.handler;

import javax.inject.Inject;
import javax.inject.Singleton;

import com.gentics.mesh.json.JsonUtil;
import com.gentics.mesh.plugin.PluginContext;
import com.gentics.mesh.plugin.common.dagger.AdminClient;
import com.gentics.mesh.plugin.common.util.ParameterUtil;
import com.gentics.mesh.plugin.forms.context.FormDataCreateContext;
import com.gentics.mesh.plugin.forms.context.FormDataUpdateContext;
import com.gentics.mesh.plugin.forms.model.CaptchaValidationException;
import com.gentics.mesh.plugin.forms.model.FormtypeConfigsModel;
import com.gentics.mesh.plugin.forms.service.FormDataService;
import com.gentics.mesh.plugin.forms.service.StatusService;
import com.gentics.mesh.rest.client.MeshRestClient;

import com.gentics.mesh.plugin.forms.service.TranslationsService;
import io.reactivex.Completable;
import io.reactivex.Single;
import io.vertx.core.http.HttpServerResponse;

@Singleton
public class SubmitDataHandler extends AbstractFormsHandler {
	private final FormDataService formDataService;
	private final TranslationsService translationService;
	private final StatusService statusService;
	private final MeshRestClient adminClient;

	@Inject
	public SubmitDataHandler(FormDataService formDataService, TranslationsService translationService, StatusService statusService,
			@AdminClient MeshRestClient adminClient) {
		this.formDataService = formDataService;
		this.translationService = translationService;
		this.statusService = statusService;
		this.adminClient = adminClient;
	}

	@Override
	public Completable handle(PluginContext context) {
		String projectName = context.project().getString("name");
		String formDataUuid = ParameterUtil.getQueryParam(context, "formDataUuid");

		Single<FormtypeConfigsModel> formtypeConfigsSingle = statusService.getFormtypeConfigs(projectName, adminClient);

		return formtypeConfigsSingle.flatMap(formtypeConfigs -> (formDataUuid == null
				? FormDataCreateContext.getCreate(formtypeConfigs, context, true, true, translationService).flatMap(saveContext -> formDataService.create(null, saveContext))
				: FormDataUpdateContext.getUpdate(formtypeConfigs, context, true, true, formDataUuid, translationService).flatMap(saveContext -> formDataService.update(null, saveContext)))
			.doOnSuccess(response -> {
				HttpServerResponse serverResponse = context.response();
				serverResponse.end(JsonUtil.toJson(response));
			}))
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
