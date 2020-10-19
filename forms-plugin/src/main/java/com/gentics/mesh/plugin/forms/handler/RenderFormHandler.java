package com.gentics.mesh.plugin.forms.handler;

import static io.vertx.core.http.HttpHeaders.CONTENT_TYPE;
import static io.vertx.ext.web.handler.TemplateHandler.DEFAULT_CONTENT_TYPE;

import java.util.List;

import javax.inject.Inject;
import javax.inject.Singleton;

import com.gentics.mesh.plugin.PluginContext;
import com.gentics.mesh.plugin.forms.service.FormService;

import io.reactivex.Completable;

@Singleton
public class RenderFormHandler extends AbstractFormsHandler {
	private final FormService formService;

	@Inject
	public RenderFormHandler(FormService formService) {
		this.formService = formService;
	}

	@Override
	public Completable handle(PluginContext context) {
		String projectName = context.project().getString("name");
		String uuid = context.pathParam("formUuid");
		List<String> languageList = context.queryParam("lang");
		String[] languages = null;
		if (languageList != null) {
			languages = (String[]) languageList.toArray(new String[languageList.size()]);
		} else {
			languages = new String[0];
		}

		return formService.render(projectName, uuid, context, languages).doOnEvent((buffer, t) -> {
			if (buffer != null) {
				context.response().putHeader(CONTENT_TYPE, String.format("%s; charset=UTF-8", DEFAULT_CONTENT_TYPE)).end(buffer.toString());
			}
		}).ignoreElement();
	}
}
