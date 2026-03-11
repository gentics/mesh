package com.gentics.mesh.plugin.forms.handler.form;

import javax.inject.Inject;
import javax.inject.Singleton;

import com.gentics.mesh.plugin.PluginContext;
import com.gentics.mesh.plugin.common.util.ParameterUtil;
import com.gentics.mesh.plugin.forms.handler.AbstractFormsHandler;
import com.gentics.mesh.plugin.forms.service.FormService;

import io.reactivex.Completable;
import io.reactivex.Single;

@Singleton
public class LoadFormHandler extends AbstractFormsHandler {

	private final FormService formService;

	@Inject
	public LoadFormHandler(FormService formService) {
		this.formService = formService;
	}

	@Override
	public Completable handle(PluginContext context) {
		String projectName = context.project().getString("name");
		Single<String> uuidSingle = ParameterUtil.getUuidFromPath(context, "formUuid");
		Single<String[]> languagesSingle = ParameterUtil.getLanguages(context);

		return Single
				.zip(uuidSingle, languagesSingle,
						(uuid, languages) -> formService.loadForm(projectName, uuid, languages))
				.flatMap(x -> x).doOnEvent((response, t) -> {
					if (response != null) {
						context.response().end(response.toString());
					}
				}).ignoreElement();
	}
}
