package com.gentics.mesh.plugin.forms.handler;

import javax.inject.Inject;
import javax.inject.Singleton;

import com.gentics.mesh.plugin.PluginContext;
import com.gentics.mesh.plugin.common.util.ParameterUtil;
import com.gentics.mesh.plugin.forms.service.FormDataService;

import io.reactivex.Completable;
import io.reactivex.Single;

@Singleton
public class DeleteFormDataHandler extends AbstractFormsHandler {

	private final FormDataService formDataService;

	@Inject
	public DeleteFormDataHandler(FormDataService formDataService) {
		this.formDataService = formDataService;
	}

	@Override
	public Completable handle(PluginContext context) {
		String projectName = context.project().getString("name");
		Single<String> formUuidSingle = ParameterUtil.getUuidFromPath(context, "formUuid");
		Single<String> formDataUuidSingle = ParameterUtil.getUuidFromPath(context, "formDataUuid");

		return Single.zip(formUuidSingle, formDataUuidSingle, (formUuid, formDataUuid) -> {
			return formDataService.delete(projectName, formUuid, formDataUuid);
		}).flatMap(x -> x).doOnEvent((response, t) -> {
			if (t == null) {
				context.response().setStatusCode(204).end();
			}
		}).ignoreElement();
	}
}
