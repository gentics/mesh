package com.gentics.mesh.plugin.forms.handler.downloads;

import com.gentics.mesh.plugin.PluginContext;
import com.gentics.mesh.plugin.common.util.HandlerUtil;
import com.gentics.mesh.plugin.forms.handler.AbstractFormsHandler;
import com.gentics.mesh.plugin.forms.service.FormDataService;
import com.gentics.mesh.plugin.forms.util.ParameterUtil;
import io.reactivex.Completable;

import javax.inject.Inject;
import javax.inject.Singleton;

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
		String formUuid = context.pathParam("formUuid");

		return formDataService
			.requestZipBinaries(projectName, formUuid, ParameterUtil.getLanguages(context))
			.doOnEvent(HandlerUtil.nodeUuidHandler(context, "downloadUuid", false))
			.ignoreElement();
	}
}
