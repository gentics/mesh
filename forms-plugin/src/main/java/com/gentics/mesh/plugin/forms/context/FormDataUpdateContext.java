package com.gentics.mesh.plugin.forms.context;

import com.gentics.mesh.plugin.PluginContext;
import com.gentics.mesh.plugin.common.MeshPluginException;
import com.gentics.mesh.plugin.common.util.ParameterUtil;

import com.gentics.mesh.plugin.forms.service.TranslationsService;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.reactivex.Single;
import org.apache.commons.lang3.StringUtils;

import java.util.Map;

/**
 * This context holds everything that the form data update handlers need.
 */
public class FormDataUpdateContext extends FormDataCreateContext {
	private final String formDataUuid;

	private static final java.util.regex.Pattern UUID_PATTERN = java.util.regex.Pattern.compile("^[0-9a-fA-F]{32}$");

	public static Single<FormDataUpdateContext> getUpdate(PluginContext pluginContext, boolean handleCookies, boolean publish, String formDataUuid, TranslationsService translationsService) {
		if (formDataUuid == null) {
			return Single.error(new MeshPluginException("FormDataUuid missing", HttpResponseStatus.BAD_REQUEST.code()));
		}
		if (!UUID_PATTERN.matcher(formDataUuid).matches()) {
			return Single.error(new MeshPluginException("Invalid formDataUuid", HttpResponseStatus.BAD_REQUEST.code()));
		}

		return ParameterUtil.getUuidFromPath(pluginContext, "formUuid")
			.flatMap(formUuid -> ParameterUtil.getFirstLanguage(pluginContext, "")
				.flatMap(lang -> StringUtils.isBlank(lang)
					? Single.error(new MeshPluginException("lang query parameter is required", HttpResponseStatus.BAD_REQUEST.code()))
					: Single.just(lang))
			.flatMap(lang -> translationsService.getTranslations(pluginContext.projectName(), formUuid, lang)
				.map(translations -> new FormDataUpdateContext(pluginContext, handleCookies, publish, formUuid, formDataUuid, lang, translations))));
	}

	protected FormDataUpdateContext(PluginContext pluginContext, boolean handleCookies, boolean publish, String formUuid, String formDataUuid, String language, Map<String, String> translations) {
		super(pluginContext, handleCookies, publish, formUuid, language, translations);
		this.formDataUuid = formDataUuid;
	}

	public String getFormDataUuid() {
		return formDataUuid;
	}
}
