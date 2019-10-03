package com.gentics.mesh.plugin.formgenerator.handler;

import static io.vertx.core.http.HttpHeaders.CONTENT_TYPE;
import static io.vertx.ext.web.handler.TemplateHandler.DEFAULT_CONTENT_TYPE;

import javax.inject.Inject;
import javax.inject.Singleton;

import com.gentics.mesh.plugin.PluginContext;
import com.gentics.mesh.plugin.formgenerator.service.FormDataService;

import io.reactivex.Completable;

@Singleton
public class RenderFormDataHandler extends AbstractFormGeneratorHandler {
	private final FormDataService formDataService;

	@Inject
	public RenderFormDataHandler(FormDataService formDataService) {
		this.formDataService = formDataService;
	}

	@Override
	public Completable handle(PluginContext context) {
		String projectName = context.project().getString("name");
		String formUuid = context.pathParam("formUuid");
		String formDataUuid = context.pathParam("formDataUuid");

		return formDataService.render(projectName, formUuid, formDataUuid, context).doOnEvent((buffer, t) -> {
			if (buffer != null) {
				context.response().putHeader(CONTENT_TYPE, String.format("%s; charset=UTF-8", DEFAULT_CONTENT_TYPE)).end(buffer.toString());
			}
		}).ignoreElement();
	}
}
