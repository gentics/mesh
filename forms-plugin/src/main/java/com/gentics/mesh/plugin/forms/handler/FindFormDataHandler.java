package com.gentics.mesh.plugin.forms.handler;

import javax.inject.Inject;
import javax.inject.Singleton;

import com.gentics.mesh.plugin.PluginContext;
import com.gentics.mesh.plugin.forms.service.FormDataService;
import com.gentics.mesh.plugin.forms.service.PagingService;

import io.reactivex.Completable;

import java.util.Optional;

@Singleton
public class FindFormDataHandler extends AbstractFormsHandler {
	private final PagingService pagingService;

	private final FormDataService formDataService;

	@Inject
	public FindFormDataHandler(PagingService pagingService, FormDataService formDataService) {
		this.pagingService = pagingService;
		this.formDataService = formDataService;
	}

	@Override
	public Completable handle(PluginContext context) {
		String projectName = context.project().getString("name");
		String formUuid = context.pathParam("formUuid");
		String query = context.request().getParam("q");
		// Return only published form data nodes by default.
		boolean publishedOnly = Optional.ofNullable(context.queryParams().get(FormDataService.PARAM_PUBLISHED_ONLY))
			.map(Boolean::parseBoolean)
			.orElse(true);

		return formDataService.find(projectName, formUuid, query, pagingService.extractPagingParams(context), publishedOnly)
			.doOnEvent((response, t) -> {
				if (response != null) {
					context.response().end(response.encodePrettily());
				}
			})
			.ignoreElement();
	}
}
