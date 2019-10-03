package com.gentics.mesh.plugin.formgenerator.handler;

import javax.inject.Inject;
import javax.inject.Singleton;

import com.gentics.mesh.plugin.PluginContext;
import com.gentics.mesh.plugin.formgenerator.config.FormGeneratorConfig;
import com.gentics.mesh.plugin.formgenerator.config.ReCaptchaOptions;
import com.gentics.mesh.plugin.formgenerator.service.FormService;

import io.reactivex.Completable;
import io.vertx.core.json.JsonObject;

@Singleton
public class LoadFormElementsHandler extends AbstractFormGeneratorHandler {

	private final FormService formService;

	private final FormGeneratorConfig config;

	@Inject
	public LoadFormElementsHandler(FormService formService, FormGeneratorConfig config) {
		this.formService = formService;
		this.config = config;
	}

	@Override
	public Completable handle(PluginContext context) {
		String projectName = context.project().getString("name");
		String uuid = context.pathParam("formUuid");
		return formService.getElements(projectName, uuid).doOnSuccess(elements -> {
			ReCaptchaOptions options = config.getReCaptcha();
			String siteKey = options == null ? null : options.getSiteKey();

			if (siteKey != null && !siteKey.isEmpty()) {
				elements.put("config", new JsonObject().put("recaptcha", new JsonObject().put("siteKey", siteKey)));
			}

			context.response().end(elements.encodePrettily());
		}).ignoreElement();
	}
}
