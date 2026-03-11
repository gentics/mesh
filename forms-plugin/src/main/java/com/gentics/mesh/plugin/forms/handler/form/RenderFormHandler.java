package com.gentics.mesh.plugin.forms.handler.form;

import static io.vertx.core.http.HttpHeaders.CONTENT_TYPE;
import static io.vertx.ext.web.handler.TemplateHandler.DEFAULT_CONTENT_TYPE;

import javax.inject.Inject;
import javax.inject.Singleton;

import com.gentics.mesh.plugin.PluginContext;
import com.gentics.mesh.plugin.common.util.ParameterUtil;
import com.gentics.mesh.plugin.forms.handler.AbstractFormsHandler;
import com.gentics.mesh.plugin.forms.service.FormService;

import io.reactivex.Completable;
import io.reactivex.Single;

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
		Single<String> uuidSingle = ParameterUtil.getUuidFromPath(context, "formUuid");
		Single<String[]> languagesSingle = ParameterUtil.getLanguages(context);

		return Single
				.zip(uuidSingle, languagesSingle,
						(uuid, languages) -> formService.render(projectName, uuid, context, languages))
				.flatMap(x -> x).doOnEvent((buffer, t) -> {
					if (buffer != null) {
						context.response()
								.putHeader(CONTENT_TYPE, String.format("%s; charset=UTF-8", DEFAULT_CONTENT_TYPE))
								.end(buffer.toString());
					}
				}).ignoreElement();
	}
}
