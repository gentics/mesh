package com.gentics.mesh.plugin.forms.handler;

import static io.vertx.core.http.HttpHeaders.CONTENT_TYPE;
import static io.vertx.ext.web.handler.TemplateHandler.DEFAULT_CONTENT_TYPE;

import javax.inject.Inject;
import javax.inject.Singleton;

import com.gentics.mesh.plugin.PluginContext;
import com.gentics.mesh.plugin.common.util.ParameterUtil;
import com.gentics.mesh.plugin.forms.service.FormDataService;

import io.reactivex.Completable;
import io.reactivex.Single;

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
		Single<String> formUuidSingle = ParameterUtil.getUuidFromPath(context, "formUuid");
		Single<String> formDataUuidSingle = ParameterUtil.getUuidFromPath(context, "formDataUuid");
		Single<String[]> languagesSingle = ParameterUtil.getLanguages(context);

		return Single.zip(formUuidSingle, formDataUuidSingle, languagesSingle, (formUuid, formDataUuid, languages) -> formDataService.render(projectName,
				formUuid, formDataUuid, context, languages)).flatMap(x -> x).doOnEvent((buffer, t) -> {
			if (buffer != null) {
				context.response().putHeader(CONTENT_TYPE, String.format("%s; charset=UTF-8", DEFAULT_CONTENT_TYPE)).end(buffer.toString());
			}
		}).ignoreElement();
	}
}
