package com.gentics.mesh.plugin.forms.handler.form;

import javax.inject.Inject;
import javax.inject.Singleton;

import com.gentics.mesh.plugin.PluginContext;
import com.gentics.mesh.plugin.forms.handler.AbstractFormsHandler;
import com.gentics.mesh.plugin.forms.service.FormService;
import com.gentics.mesh.plugin.forms.util.ParameterUtil;

import io.reactivex.Completable;

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
		String uuid = context.pathParam("formUuid");
		return formService.loadForm(projectName, uuid, ParameterUtil.getLanguages(context)).doOnEvent((response, t) -> {
			if (response != null) {
				context.response().end(response.toString());
			}
		}).ignoreElement();
	}
}
