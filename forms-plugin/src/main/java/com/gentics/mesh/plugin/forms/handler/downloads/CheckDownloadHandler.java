package com.gentics.mesh.plugin.forms.handler.downloads;

import com.gentics.mesh.plugin.PluginContext;
import com.gentics.mesh.plugin.common.util.HandlerUtil;
import com.gentics.mesh.plugin.forms.config.FormsConfig;
import com.gentics.mesh.plugin.forms.handler.AbstractFormsHandler;
import com.gentics.mesh.plugin.forms.service.FormDataService;
import com.gentics.mesh.plugin.forms.util.ParameterUtil;
import io.reactivex.Completable;

import javax.inject.Inject;

/**
 * Check if the requested download is ready and if so return the download URL.
 *
 * <p>
 *     The response will also contain information if another download is
 *     currently being prepared.
 * </p>
 */
public class CheckDownloadHandler extends AbstractFormsHandler {
	private final FormDataService formDataService;
	private final FormsConfig configuration;

	@Inject
	public CheckDownloadHandler(FormDataService formDataService, FormsConfig configuration) {
		this.formDataService = formDataService;
		this.configuration = configuration;
	}

	@Override
	public Completable handle(PluginContext context) {
		String projectName = context.project().getString("name");
		String formUuid = context.pathParam("formUuid");
		String downloadType = context.pathParam("downloadType");
		String options = "export".equals(downloadType) ? ParameterUtil.getExportOptions(context, configuration) : "";

		return formDataService
			.checkDownload(projectName, formUuid, options, ParameterUtil.getLanguages(context), downloadType)
			.doOnEvent(HandlerUtil.jsonObjectHandler(context, false))
			.ignoreElement();
	}
}
