package com.gentics.mesh.plugin.forms.handler;

import javax.inject.Inject;
import javax.inject.Singleton;

import com.gentics.mesh.plugin.PluginContext;
import com.gentics.mesh.plugin.forms.context.FormDataCreateContext;
import com.gentics.mesh.plugin.forms.service.FormDataService;

import io.reactivex.Completable;

@Singleton
public class PostDataHandler extends AbstractFormsHandler {
	private final FormDataService formDataService;

	@Inject
	public PostDataHandler(FormDataService formDataService) {
		this.formDataService = formDataService;
	}

	@Override
	public Completable handle(PluginContext context) {
		return FormDataCreateContext.getCreate(context, true).flatMap(saveContext -> formDataService.create(saveContext))
			.doOnSuccess(response -> {
				String redirectUrl = response.getRedirectUrl();
				if (redirectUrl != null && !"".equals(redirectUrl)) {
					context.response().setStatusCode(303).putHeader("Location", redirectUrl).end();
				} else {
					context.response().setStatusCode(205).end();
				}
			})
			.ignoreElement();
	}
}
