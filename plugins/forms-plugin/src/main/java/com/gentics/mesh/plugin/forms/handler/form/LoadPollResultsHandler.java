package com.gentics.mesh.plugin.forms.handler.form;

import com.gentics.mesh.plugin.PluginContext;
import com.gentics.mesh.plugin.common.dagger.AdminClient;
import com.gentics.mesh.plugin.common.util.ParameterUtil;
import com.gentics.mesh.plugin.forms.handler.AbstractFormsHandler;
import com.gentics.mesh.plugin.forms.model.FormtypeConfigsModel;
import com.gentics.mesh.plugin.forms.service.FormService;
import com.gentics.mesh.plugin.forms.service.StatusService;
import com.gentics.mesh.rest.client.MeshRestClient;

import io.reactivex.Completable;
import io.reactivex.Single;

import javax.inject.Inject;

/**
 * Load poll results as JSON.
 */
public class LoadPollResultsHandler extends AbstractFormsHandler {

	private final FormService formService;
	private final StatusService statusService;
	private final MeshRestClient adminClient;

	@Inject
	public LoadPollResultsHandler(FormService formService, StatusService statusService,
			@AdminClient MeshRestClient adminClient) {
		this.formService = formService;
		this.statusService = statusService;
		this.adminClient = adminClient;
	}

	@Override
	public Completable handle(PluginContext context) {
		String projectName = context.project().getString("name");
		Single<String> singleformUuid = ParameterUtil.getUuidFromPath(context, "formUuid");
		Single<FormtypeConfigsModel> formtypeConfigsSingle = statusService.getFormtypeConfigs(projectName, adminClient);
		Single<String> languageSingle = ParameterUtil.getFirstLanguage(context, "");

		return Single.zip(singleformUuid, formtypeConfigsSingle, languageSingle, (formUuid, formtypeConfigs, language) -> formService.loadPollResults(formtypeConfigs, context, formUuid, language))
			.flatMap(x -> x)
			.doOnSuccess(results -> context.response().end(results.encodePrettily()))
			.ignoreElement();
	}
}
