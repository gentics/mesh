package com.gentics.mesh.plugin.forms.handler;

import javax.inject.Inject;
import javax.inject.Singleton;

import com.gentics.mesh.plugin.PluginContext;
import com.gentics.mesh.plugin.forms.service.FormDataService;

import io.netty.handler.codec.http.HttpResponseStatus;
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

		return formDataService.load(projectName, formUuid, formDataUuid).doOnEvent((response, t) -> {
			if (response != null) {

				// Loading form data is only allowed for forms which are not yet published.
				if (response.getVersion().endsWith(".0")) {
					context.response()
						.setStatusCode(HttpResponseStatus.NOT_FOUND.code())
						.end();
				} else {
					context.response().end(response.toJson());
				}
			}
		}).ignoreElement();
	}
}
