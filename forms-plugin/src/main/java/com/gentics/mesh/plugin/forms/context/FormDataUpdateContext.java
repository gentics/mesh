package com.gentics.mesh.plugin.forms.context;

import com.gentics.mesh.plugin.PluginContext;
import com.gentics.mesh.plugin.common.MeshPluginException;
import com.gentics.mesh.plugin.common.util.ParameterUtil;

import io.reactivex.Single;

/**
 * This context holds everything that the form data update handlers need.
 */
public class FormDataUpdateContext extends FormDataCreateContext {
	private final String formDataUuid;

	private static final java.util.regex.Pattern UUID_PATTERN = java.util.regex.Pattern.compile("^[0-9a-fA-F]{32}$");

	public static Single<FormDataUpdateContext> getUpdate(PluginContext pluginContext, boolean handleCookies, boolean publish, String formDataUuid) {
		if (formDataUuid == null) {
			return Single.error(new MeshPluginException("FormDataUuid missing", 400));
		}
		if (!UUID_PATTERN.matcher(formDataUuid).matches()) {
			return Single.error(new MeshPluginException("Invalid formDataUuid", 400));
		}

		Single<String> formUuidSingle = ParameterUtil.getUuidFromPath(pluginContext, "formUuid");
		Single<String> languageSingle = ParameterUtil.getFirstLanguage(pluginContext, "")
				.flatMap(lang -> lang.isEmpty()
						? Single.error(new MeshPluginException("lang query parameter is required", 400))
						: Single.just(lang));

		return Single.zip(formUuidSingle, languageSingle,
				(formUuid, language) -> new FormDataUpdateContext(pluginContext, handleCookies, publish, formUuid, formDataUuid, language));
	}

	protected FormDataUpdateContext(PluginContext pluginContext, boolean handleCookies, boolean publish, String formUuid, String formDataUuid, String language) {
		super(pluginContext, handleCookies, publish, formUuid, language);
		this.formDataUuid = formDataUuid;
	}

	public String getFormDataUuid() {
		return formDataUuid;
	}
}
