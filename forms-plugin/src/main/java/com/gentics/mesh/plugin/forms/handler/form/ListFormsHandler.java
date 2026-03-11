package com.gentics.mesh.plugin.forms.handler.form;

import javax.inject.Inject;
import javax.inject.Singleton;

import com.gentics.mesh.plugin.PluginContext;
import com.gentics.mesh.plugin.common.util.ParameterUtil;
import com.gentics.mesh.plugin.forms.handler.AbstractFormsHandler;
import com.gentics.mesh.plugin.forms.service.FormService;
import com.gentics.mesh.plugin.forms.service.PagingService;

import io.reactivex.Completable;
import io.reactivex.Single;
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
		Single<String[]> languagesSingle = ParameterUtil.getLanguages(context);

		return languagesSingle
				.flatMap(languages -> formService.list(context.project().getString("name"), pagingParams, languages))
				.doOnEvent((response, t) -> {
					if (response != null) {
						context.response().end(response.encodePrettily());
					}
				}).ignoreElement();
	}
}
