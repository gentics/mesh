package com.gentics.mesh.plugin.forms.handler.form;

import com.gentics.mesh.plugin.PluginContext;
import com.gentics.mesh.plugin.common.util.ParameterUtil;
import com.gentics.mesh.plugin.forms.handler.AbstractFormsHandler;
import com.gentics.mesh.plugin.forms.service.FormService;
import io.reactivex.Completable;

import javax.inject.Inject;

/**
 * Load poll results as JSON.
 */
public class LoadPollResultsHandler extends AbstractFormsHandler {

	private final FormService formService;

	@Inject
	public LoadPollResultsHandler(FormService formService) {
		this.formService = formService;
	}

	@Override
	public Completable handle(PluginContext context) {
		return ParameterUtil.getUuidFromPath(context, "formUuid")
			.flatMap(formUuid -> formService.loadPollResults(context, formUuid))
			.doOnSuccess(results -> context.response().end(results.encodePrettily()))
			.ignoreElement();
	}
}
