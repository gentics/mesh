package com.gentics.mesh.plugin.forms.handler;

import static io.vertx.core.http.HttpHeaders.CONTENT_TYPE;
import static io.vertx.ext.web.handler.TemplateHandler.DEFAULT_CONTENT_TYPE;

import java.util.List;

import javax.inject.Inject;
import javax.inject.Singleton;

import com.gentics.mesh.plugin.PluginContext;
import com.gentics.mesh.plugin.forms.service.FormDataService;

import io.reactivex.Completable;

@Singleton
public class RenderFormDataHandler extends AbstractFormsHandler {
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
		List<String> languageList = context.queryParam("lang");
		String[] languages = null;
		if (languageList != null) {
			languages = (String[]) languageList.toArray(new String[languageList.size()]);
		} else {
			languages = new String[0];
		}

		return formDataService.render(projectName, formUuid, formDataUuid, context, languages).doOnEvent((buffer, t) -> {
			if (buffer != null) {
				context.response().putHeader(CONTENT_TYPE, String.format("%s; charset=UTF-8", DEFAULT_CONTENT_TYPE)).end(buffer.toString());
			}
		}).ignoreElement();
	}
}
