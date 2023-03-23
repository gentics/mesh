package com.gentics.mesh.plugin.forms.handler.form;

import javax.inject.Inject;
import javax.inject.Singleton;

import com.gentics.mesh.plugin.PluginContext;
import com.gentics.mesh.plugin.forms.handler.AbstractFormsHandler;
import com.gentics.mesh.plugin.forms.service.FormService;
import com.gentics.mesh.plugin.forms.service.PagingService;

import com.gentics.mesh.plugin.forms.util.ParameterUtil;
import io.reactivex.Completable;
import io.vertx.core.json.JsonObject;

@Singleton
public class ListFormsHandler extends AbstractFormsHandler {
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
		String[] languages = ParameterUtil.getLanguages(context);

		return formService.list(context.project().getString("name"), pagingParams, languages).doOnEvent((response, t) -> {
			if (response != null) {
				context.response().end(response.encodePrettily());
			}
		}).ignoreElement();
	}
}
