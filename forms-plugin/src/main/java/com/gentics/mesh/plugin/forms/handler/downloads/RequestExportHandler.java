package com.gentics.mesh.plugin.forms.handler.downloads;

import javax.inject.Inject;
import javax.inject.Singleton;

import com.gentics.mesh.plugin.PluginContext;
import com.gentics.mesh.plugin.common.util.HandlerUtil;
import com.gentics.mesh.plugin.common.util.ParameterUtil;
import com.gentics.mesh.plugin.forms.handler.AbstractFormsHandler;
import com.gentics.mesh.plugin.forms.service.FormDataService;

import io.reactivex.Completable;
import io.reactivex.Single;
import io.vertx.core.Vertx;

/**
 * Handler for requesting creation of an export.
 */
@Singleton
public class RequestExportHandler extends AbstractFormsHandler {
	private final FormDataService formDataService;

	@Inject
	public RequestExportHandler(FormDataService formDataService, Vertx vertx) {
		this.formDataService = formDataService;
	}

	@Override
	public Completable handle(PluginContext context) {
		String projectName = context.project().getString("name");
		Single<String> formUuidSingle = ParameterUtil.getUuidFromPath(context, "formUuid");
		Single<String[]> languagesSingle = ParameterUtil.getLanguages(context);

		return Single
				.zip(formUuidSingle, languagesSingle,
						(formUuid, languages) -> formDataService.requestExport(projectName, formUuid, context,
								languages))
				.flatMap(x -> x).doOnEvent(HandlerUtil.nodeUuidHandler(context, "downloadUuid", false)).ignoreElement();
	}
}
