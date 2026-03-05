package com.gentics.mesh.plugin.forms.handler.form;

import javax.inject.Inject;
import javax.inject.Singleton;

import com.gentics.mesh.plugin.PluginContext;
import com.gentics.mesh.plugin.common.util.ParameterUtil;
import com.gentics.mesh.plugin.forms.config.FormsConfig;
import com.gentics.mesh.plugin.forms.config.ReCaptchaOptions;
import com.gentics.mesh.plugin.forms.handler.AbstractFormsHandler;
import com.gentics.mesh.plugin.forms.service.FormService;

import io.reactivex.Completable;
import io.reactivex.Single;
import io.vertx.core.json.JsonObject;

@Singleton
public class LoadFormElementsHandler extends AbstractFormsHandler {

	private final FormService formService;

	private final FormsConfig config;

	@Inject
	public LoadFormElementsHandler(FormService formService, FormsConfig config) {
		this.formService = formService;
		this.config = config;
	}

	@Override
	public Completable handle(PluginContext context) {
		String projectName = context.project().getString("name");
		Single<String> uuidSingle = ParameterUtil.getUuidFromPath(context, "formUuid");
		Single<String[]> languagesSingle = ParameterUtil.getLanguages(context);

		return Single
				.zip(uuidSingle, languagesSingle,
						(uuid, languages) -> formService.getElements(projectName, uuid, languages))
				.flatMap(x -> x).doOnSuccess(elements -> {
					ReCaptchaOptions options = config.getReCaptcha();
					String siteKey = options == null ? null : options.getSiteKey();

					if (siteKey != null && !siteKey.isEmpty()) {
						elements.put("config",
								new JsonObject().put("recaptcha", new JsonObject().put("siteKey", siteKey)));
					}

					context.response().end(elements.encodePrettily());
				}).ignoreElement();
	}
}
