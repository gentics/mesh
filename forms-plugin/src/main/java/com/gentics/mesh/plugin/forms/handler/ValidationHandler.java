package com.gentics.mesh.plugin.forms.handler;

import javax.inject.Inject;
import javax.inject.Singleton;
import javax.swing.TransferHandler;

import com.gentics.mesh.json.JsonUtil;
import com.gentics.mesh.plugin.PluginContext;
import com.gentics.mesh.plugin.common.dagger.AdminClient;
import com.gentics.mesh.plugin.common.util.ParameterUtil;
import com.gentics.mesh.plugin.forms.context.FormDataCreateContext;
import com.gentics.mesh.plugin.forms.context.FormDataUpdateContext;
import com.gentics.mesh.plugin.forms.model.FormtypeConfigsModel;
import com.gentics.mesh.plugin.forms.service.FormDataService;
import com.gentics.mesh.plugin.forms.service.StatusService;
import com.gentics.mesh.rest.client.MeshRestClient;

import com.gentics.mesh.plugin.forms.service.TranslationsService;
import io.reactivex.Completable;
import io.reactivex.Single;

@Singleton
public class ValidationHandler extends AbstractFormsHandler {
	private final FormDataService formDataService;
	private final TranslationsService translationService;
	private final StatusService statusService;
	private final MeshRestClient adminClient;

	@Inject
	public ValidationHandler(FormDataService formDataService, TranslationsService translationService, StatusService statusService,
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
				? FormDataCreateContext.getCreate(formtypeConfigs, context, false, false, translationService).flatMap(c -> formDataService.validate(formtypeConfigs, c))
				: FormDataUpdateContext.getUpdate(formtypeConfigs, context, false, false, formDataUuid, translationService).flatMap(c -> formDataService.validate(formtypeConfigs, c))))
			.doOnSuccess(response -> context.response().end(JsonUtil.toJson(response)))
			.ignoreElement();
	}
}
