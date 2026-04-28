package com.gentics.mesh.plugin.forms.handler.form;

import javax.inject.Inject;
import javax.inject.Singleton;

import com.gentics.mesh.http.HttpConstants;
import com.gentics.mesh.plugin.PluginContext;
import com.gentics.mesh.plugin.common.util.ParameterUtil;
import com.gentics.mesh.plugin.forms.handler.AbstractFormsHandler;
import com.gentics.mesh.plugin.forms.service.FormService;

import io.reactivex.Completable;
import io.reactivex.Single;
import io.vertx.core.http.HttpHeaders;

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

		return uuidSingle
				.flatMap(uuid -> formService.loadForm(projectName, uuid))
				.doOnEvent((response, t) -> {
					if (response != null) {
						context.response().putHeader(HttpHeaders.CONTENT_TYPE, HttpConstants.APPLICATION_JSON_UTF8)
								.end(response.toString());
					}
				}).ignoreElement();
	}
}
