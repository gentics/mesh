package com.gentics.mesh.plugin.forms.handler;

import javax.inject.Inject;
import javax.inject.Singleton;

import com.gentics.mesh.plugin.PluginContext;
import com.gentics.mesh.plugin.common.util.HandlerUtil;
import com.gentics.mesh.plugin.common.util.ParameterUtil;
import com.gentics.mesh.plugin.forms.service.FormDataService;

import io.reactivex.Completable;
import io.reactivex.Single;

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
		Single<String> formUuidSingle = ParameterUtil.getUuidFromPath(context, "formUuid");
		Single<String> formDataUuidSingle = ParameterUtil.getUuidFromPath(context, "formDataUuid");

		return Single.zip(formUuidSingle, formDataUuidSingle, (formUuid, formDataUuid) -> formDataService.load(projectName, formUuid, formDataUuid))
			.flatMap(x -> x)
			.doOnEvent(HandlerUtil.jsonHandler(context, false))
			.ignoreElement();
	}
}
