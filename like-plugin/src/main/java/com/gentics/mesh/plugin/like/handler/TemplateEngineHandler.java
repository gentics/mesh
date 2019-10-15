package com.gentics.mesh.plugin.like.handler;

import javax.inject.Inject;
import javax.inject.Singleton;

import com.gentics.mesh.plugin.PluginContext;
import com.gentics.mesh.plugin.like.service.TemplateService;

import io.reactivex.Completable;

@Singleton
public class TemplateEngineHandler extends AbstractLikeHandler {
	private final TemplateService templateService;

	@Inject
	public TemplateEngineHandler(TemplateService templateService) {
		this.templateService = templateService;
	}

	@Override
	public Completable handle(PluginContext context) {
		return templateService.getJavascript().doOnSuccess(out -> context.response().end(out)).ignoreElement();
	}
}
