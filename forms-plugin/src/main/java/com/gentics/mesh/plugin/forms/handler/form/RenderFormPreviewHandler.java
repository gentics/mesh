package com.gentics.mesh.plugin.forms.handler.form;

import com.gentics.mesh.plugin.PluginContext;
import com.gentics.mesh.plugin.forms.handler.AbstractFormsHandler;
import com.gentics.mesh.plugin.forms.service.FormService;
import io.reactivex.Completable;

import javax.inject.Inject;
import javax.inject.Singleton;

import static io.vertx.core.http.HttpHeaders.CONTENT_TYPE;
import static io.vertx.ext.web.handler.TemplateHandler.DEFAULT_CONTENT_TYPE;

@Singleton
public class RenderFormPreviewHandler extends AbstractFormsHandler {
	private final FormService formService;

	@Inject
	public RenderFormPreviewHandler(FormService formService) {
		this.formService = formService;
	}

	@Override
	public Completable handle(PluginContext context) {
		String uuid = context.pathParam("formUuid");

		return formService.renderForm(uuid, context.getBodyAsString(), null, context).doOnEvent((buffer, t) -> {
			if (buffer != null) {
				context.response().putHeader(CONTENT_TYPE, String.format("%s; charset=UTF-8", DEFAULT_CONTENT_TYPE)).end(buffer.toString());
			}
		}).ignoreElement();
	}
}
