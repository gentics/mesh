package com.gentics.mesh.plugin.comment.handler;

import com.gentics.mesh.plugin.PluginContext;
import com.gentics.mesh.plugin.comment.service.TemplateService;
import io.reactivex.Completable;

import javax.inject.Inject;

public class TemplateEngineHandler extends AbstractCommentHandler {
	private final TemplateService templateService;

	@Inject
	public TemplateEngineHandler(TemplateService templateService) {
		this.templateService = templateService;
	}

	@Override
	public Completable handle(PluginContext context) {
		return templateService.getJavascript()
			.doOnEvent(stringHandler(context))
			.ignoreElement();
	}
}
