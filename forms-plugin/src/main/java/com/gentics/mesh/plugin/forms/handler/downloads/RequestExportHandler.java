package com.gentics.mesh.plugin.forms.handler.downloads;

import javax.inject.Inject;
import javax.inject.Singleton;

import com.gentics.mesh.plugin.PluginContext;
import com.gentics.mesh.plugin.common.dagger.AdminClient;
import com.gentics.mesh.plugin.common.util.HandlerUtil;
import com.gentics.mesh.plugin.common.util.ParameterUtil;
import com.gentics.mesh.plugin.forms.handler.AbstractFormsHandler;
import com.gentics.mesh.plugin.forms.model.FormtypeConfigsModel;
import com.gentics.mesh.plugin.forms.service.FormDataService;
import com.gentics.mesh.plugin.forms.service.StatusService;
import com.gentics.mesh.rest.client.MeshRestClient;

import io.reactivex.Completable;
import io.reactivex.Single;

/**
 * Handler for requesting creation of an export.
 */
@Singleton
public class RequestExportHandler extends AbstractFormsHandler {
	private final FormDataService formDataService;
	private final StatusService statusService;
	private final MeshRestClient adminClient;

	@Inject
	public RequestExportHandler(FormDataService formDataService, StatusService statusService,
			@AdminClient MeshRestClient adminClient) {
		this.formDataService = formDataService;
		this.statusService = statusService;
		this.adminClient = adminClient;
	}

	@Override
	public Completable handle(PluginContext context) {
		String projectName = context.project().getString("name");
		Single<String> formUuidSingle = ParameterUtil.getUuidFromPath(context, "formUuid");
		Single<String> languagesSingle = ParameterUtil.getFirstLanguage(context, "");
		Single<FormtypeConfigsModel> formtypeConfigsSingle = statusService.getFormtypeConfigs(projectName, adminClient);

		return Single
				.zip(formUuidSingle, languagesSingle, formtypeConfigsSingle,
						(formUuid, language, formtypeConfigs) -> formDataService.requestExport(formtypeConfigs, projectName, formUuid, context, language))
				.flatMap(x -> x).doOnEvent(HandlerUtil.nodeUuidHandler(context, "downloadUuid", false)).ignoreElement();
	}
}
