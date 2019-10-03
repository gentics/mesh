package com.gentics.mesh.plugin.formgenerator.handler;

import javax.inject.Inject;
import javax.inject.Singleton;

import com.gentics.mesh.plugin.PluginContext;
import com.gentics.mesh.plugin.formgenerator.service.FormDataService;
import com.gentics.mesh.plugin.formgenerator.service.PagingService;

import io.reactivex.Completable;

@Singleton
public class FindFormDataHandler extends AbstractFormGeneratorHandler {
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

		return formDataService.find(projectName, formUuid, query, pagingService.extractPagingParams(context)).doOnEvent((response, t) -> {
			if (response != null) {
				context.response().end(response.encodePrettily());
			}
		}).ignoreElement();
	}
}
