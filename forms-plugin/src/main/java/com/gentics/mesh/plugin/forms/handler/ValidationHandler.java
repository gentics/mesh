package com.gentics.mesh.plugin.forms.handler;

import javax.inject.Inject;
import javax.inject.Singleton;
import javax.swing.TransferHandler;

import com.gentics.mesh.json.JsonUtil;
import com.gentics.mesh.plugin.PluginContext;
import com.gentics.mesh.plugin.common.util.ParameterUtil;
import com.gentics.mesh.plugin.forms.context.FormDataCreateContext;
import com.gentics.mesh.plugin.forms.context.FormDataUpdateContext;
import com.gentics.mesh.plugin.forms.service.FormDataService;

import com.gentics.mesh.plugin.forms.service.TranslationsService;
import io.reactivex.Completable;

@Singleton
public class ValidationHandler extends AbstractFormsHandler {
	private final FormDataService formDataService;
	private final TranslationsService translationService;

	@Inject
	public ValidationHandler(FormDataService formDataService, TranslationsService translationService) {
		this.formDataService = formDataService;
		this.translationService = translationService;
	}

	@Override
	public Completable handle(PluginContext context) {
		String formDataUuid = ParameterUtil.getQueryParam(context, "formDataUuid");
		return (formDataUuid == null
				? FormDataCreateContext.getCreate(context, false, false, translationService).flatMap(formDataService::validate)
				: FormDataUpdateContext.getUpdate(context, false, false, formDataUuid, translationService).flatMap(formDataService::validate))
			.doOnSuccess(response -> context.response().end(JsonUtil.toJson(response)))
			.ignoreElement();
	}
}
