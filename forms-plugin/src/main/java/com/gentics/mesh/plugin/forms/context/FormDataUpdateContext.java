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

	public static Single<FormDataUpdateContext> getUpdate(PluginContext pluginContext, boolean handleCookies, boolean publish, String formDataUuid) {
		if (formDataUuid == null) {
			return Single.error(new MeshPluginException("FormDataUuid missing", 400));
		}

		Single<String> formUuidSingle = ParameterUtil.getUuidFromPath(pluginContext, "formUuid");
		Single<String[]> languagesSingle = ParameterUtil.getLanguages(pluginContext);

		return Single.zip(formUuidSingle, languagesSingle,
				(formUuid, languages) -> new FormDataUpdateContext(pluginContext, handleCookies, publish, formUuid, formDataUuid, languages));
	}

	protected FormDataUpdateContext(PluginContext pluginContext, boolean handleCookies, boolean publish, String formUuid, String formDataUuid, String[] languages) {
		super(pluginContext, handleCookies, publish, formUuid, languages);
		this.formDataUuid = formDataUuid;
	}

	public String getFormDataUuid() {
		return formDataUuid;
	}
}
