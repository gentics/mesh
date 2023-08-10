package com.gentics.mesh.plugin.forms.handler;

import javax.inject.Inject;
import javax.inject.Singleton;

import com.gentics.mesh.plugin.PluginContext;
import com.gentics.mesh.plugin.common.util.HandlerUtil;
import com.gentics.mesh.plugin.forms.service.FormDataService;

import io.reactivex.Completable;

@Singleton
public class LoadFormDataHandler extends AbstractFormsHandler {
	private final FormDataService formDataService;

	@Inject
	public LoadFormDataHandler(FormDataService formDataService) {
		this.formDataService = formDataService;
	}

	@Override
	public Completable handle(PluginContext context) {
		String projectName = context.project().getString("name");
		String formUuid = context.pathParam("formUuid");
		String formDataUuid = context.pathParam("formDataUuid");


		return formDataService.load(projectName, formUuid, formDataUuid)
			.doOnEvent(HandlerUtil.jsonHandler(context, false))
			.ignoreElement();
	}
}
