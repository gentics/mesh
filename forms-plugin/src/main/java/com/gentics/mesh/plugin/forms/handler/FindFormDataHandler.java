package com.gentics.mesh.plugin.forms.handler;

import java.util.Optional;

import javax.inject.Inject;
import javax.inject.Singleton;

import com.gentics.mesh.plugin.PluginContext;
import com.gentics.mesh.plugin.common.dagger.AdminClient;
import com.gentics.mesh.plugin.common.util.ParameterUtil;
import com.gentics.mesh.plugin.forms.model.FormtypeConfigsModel;
import com.gentics.mesh.plugin.forms.service.FormDataService;
import com.gentics.mesh.plugin.forms.service.PagingService;
import com.gentics.mesh.plugin.forms.service.StatusService;
import com.gentics.mesh.rest.client.MeshRestClient;

import io.reactivex.Completable;
import io.reactivex.Single;

//TODO 8.5 needed? 
@Singleton
public class FindFormDataHandler extends AbstractFormsHandler {
	private final PagingService pagingService;

	private final FormDataService formDataService;
	private final StatusService statusService;
	private final MeshRestClient adminClient;

	@Inject
	public FindFormDataHandler(PagingService pagingService, FormDataService formDataService, StatusService statusService,
			@AdminClient MeshRestClient adminClient) {
		this.pagingService = pagingService;
		this.formDataService = formDataService;
		this.statusService = statusService;
		this.adminClient = adminClient;
	}

	@Override
	public Completable handle(PluginContext context) {
		String projectName = context.project().getString("name");

		Single<String> formUuidSingle = ParameterUtil.getUuidFromPath(context, "formUuid");
		Single<FormtypeConfigsModel> formtypeConfigsSingle = statusService.getFormtypeConfigs(projectName, adminClient);

		String query = context.request().getParam("q");
		// Return only published form data nodes by default.
		boolean publishedOnly = Optional.ofNullable(context.queryParams().get(FormDataService.PARAM_PUBLISHED_ONLY))
			.map(Boolean::parseBoolean)
			.orElse(true);

		return Single.zip(formUuidSingle, formtypeConfigsSingle, (formUuid, formtypeConfigs) -> formDataService.find(formtypeConfigs, projectName, formUuid, query,
				pagingService.extractPagingParams(context), publishedOnly)).flatMap(x -> x).doOnEvent((response, t) -> {
					if (response != null) {
						context.response().end(response.encodePrettily());
					}
				}).ignoreElement();
	}
}
