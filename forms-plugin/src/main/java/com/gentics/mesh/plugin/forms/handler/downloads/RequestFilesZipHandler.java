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

/**
 * Handler for requesting creation of an export.
 */
@Singleton
public class RequestFilesZipHandler extends AbstractFormsHandler {
	private final FormDataService formDataService;

	@Inject
	public RequestFilesZipHandler(FormDataService formDataService) {
		this.formDataService = formDataService;
	}

	@Override
	public Completable handle(PluginContext context) {
		String projectName = context.project().getString("name");
		Single<String> formUuidSingle = ParameterUtil.getUuidFromPath(context, "formUuid");
		Single<String[]> languagesSingle = ParameterUtil.getLanguages(context);

		return Single
				.zip(formUuidSingle, languagesSingle,
						(formUuid, languages) -> formDataService.requestZipBinaries(projectName, formUuid, languages))
				.flatMap(x -> x).doOnEvent(HandlerUtil.nodeUuidHandler(context, "downloadUuid", false)).ignoreElement();
	}
}
