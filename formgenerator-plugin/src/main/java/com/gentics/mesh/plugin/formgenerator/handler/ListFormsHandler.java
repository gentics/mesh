package com.gentics.mesh.plugin.formgenerator.handler;

import javax.inject.Inject;
import javax.inject.Singleton;

import com.gentics.mesh.plugin.PluginContext;
import com.gentics.mesh.plugin.formgenerator.service.FormService;
import com.gentics.mesh.plugin.formgenerator.service.PagingService;

import io.reactivex.Completable;
import io.vertx.core.json.JsonObject;

@Singleton
public class ListFormsHandler extends AbstractFormGeneratorHandler {
	private final PagingService pagingService;

	private final FormService formService;

	@Inject
	public ListFormsHandler(PagingService pagingService, FormService formService) {
		this.pagingService = pagingService;
		this.formService = formService;
	}

	@Override
	public Completable handle(PluginContext context) {
		JsonObject pagingParams = pagingService.extractPagingParams(context);

		return formService.list(context.project().getString("name"), pagingParams).doOnEvent((response, t) -> {
			if (response != null) {
				context.response().end(response.encodePrettily());
			}
		}).ignoreElement();
	}
}
