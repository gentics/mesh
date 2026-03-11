package com.gentics.mesh.plugin.forms.handler;

import javax.inject.Inject;
import javax.inject.Singleton;

import com.gentics.mesh.plugin.PluginContext;
import com.gentics.mesh.plugin.forms.context.FormDataUpdateContext;
import com.gentics.mesh.plugin.forms.service.FormDataService;

import io.reactivex.Completable;

@Singleton
public class UpdatePostedDataHandler extends AbstractFormsHandler {
	private final FormDataService formDataService;

	@Inject
	public UpdatePostedDataHandler(FormDataService formDataService) {
		this.formDataService = formDataService;
	}

	@Override
	public Completable handle(PluginContext context) {
		return FormDataUpdateContext.getUpdate(context, true).flatMap(saveContext -> formDataService.update(saveContext))
				.doOnSuccess(response -> {
					String redirectUrl = response.getRedirectUrl();
					if (redirectUrl != null && !"".equals(redirectUrl)) {
						context.response().setStatusCode(303).putHeader("Location", redirectUrl).end();
					} else {
						context.response().setStatusCode(205).end();
					}
				}).ignoreElement();
	}
}
