package com.gentics.mesh.plugin.forms.context;

import com.gentics.mesh.plugin.PluginContext;
import com.gentics.mesh.plugin.common.util.ParameterUtil;

import io.reactivex.Single;

/**
 * This context holds everything that the form data update handlers need.
 */
public class FormDataUpdateContext extends FormDataCreateContext {
	private final String formDataUuid;

	/**
	 * Get a Single that emits the instance or an error if validation on any of the parameters fails
	 * @param pluginContext plugin context
	 * @param handleCookies true to handle cookies
	 * @return single
	 */
	public static Single<FormDataUpdateContext> getUpdate(PluginContext pluginContext, boolean handleCookies) {
		Single<String> formUuidSingle = ParameterUtil.getUuidFromPath(pluginContext, "formUuid");
		Single<String> formDataUuidSingle = ParameterUtil.getUuidFromPath(pluginContext, "formDataUuid");
		Single<String[]> languagesSingle = ParameterUtil.getLanguages(pluginContext);

		return Single.zip(formUuidSingle, formDataUuidSingle, languagesSingle, (formUuid, formDataUuid,
				languages) -> new FormDataUpdateContext(pluginContext, handleCookies, formUuid, formDataUuid, languages));
	}

	protected FormDataUpdateContext(PluginContext pluginContext, boolean handleCookies, String formUuid, String formDataUuid, String[] languages) {
		super(pluginContext, handleCookies, formUuid, languages);
		this.formDataUuid = formDataUuid;
	}

	public String getFormDataUuid() {
		return formDataUuid;
	}
}
