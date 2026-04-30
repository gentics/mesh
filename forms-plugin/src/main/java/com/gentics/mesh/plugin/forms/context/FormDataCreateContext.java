package com.gentics.mesh.plugin.forms.context;

import java.util.HashSet;
import java.util.Set;

import com.gentics.mesh.core.rest.node.NodeResponse;
import com.gentics.mesh.plugin.PluginContext;
import com.gentics.mesh.plugin.common.util.ParameterUtil;
import com.gentics.mesh.plugin.forms.model.Form;

import io.reactivex.Single;
import io.vertx.core.MultiMap;
import io.vertx.ext.web.FileUpload;

/**
 * This context holds everything that the form data create handlers need.
 */
public class FormDataCreateContext extends FormDataValidateContext {

	private final boolean handleCookies;
	private final boolean publish;
	private final String formUuid;
	private final String[] languages;

	/**
	 * Get a Single that emits the instance or an error if validation on any of the parameters fails
	 * @param pluginContext plugin context
	 * @param handleCookies true to handle cookies
	 * @return single
	 */
	public static Single<FormDataCreateContext> getCreate(PluginContext pluginContext, boolean handleCookies, boolean publish) {
		Single<String> formUuidSingle = ParameterUtil.getUuidFromPath(pluginContext, "formUuid");
		Single<String[]> languagesSingle = ParameterUtil.getLanguages(pluginContext);

		return Single.zip(formUuidSingle, languagesSingle,
				(formUuid, languages) -> new FormDataCreateContext(pluginContext, handleCookies, publish, formUuid, languages));
	}

	protected FormDataCreateContext(PluginContext pluginContext, boolean handleCookies, boolean publish, String formUuid, String[] languages) {
		super(pluginContext);
		this.handleCookies = handleCookies;
		this.publish = publish;
		this.formUuid = formUuid;
		this.languages = languages;
	}

	public String getProjectName() {
		return getContext().project().getString("name");
	}

	public String getFormUuid() {
		return formUuid;
	}

	@Override
	public FormDataCreateContext setForm(Form form) {
		super.setForm(form);
		return this;
	}

	@Override
	public FormDataCreateContext setFormDataNode(NodeResponse formDataNode) {
		super.setFormDataNode(formDataNode);
		return this;
	}

	@Override
	public FormDataCreateContext setFormNode(NodeResponse formNode) {
		super.setFormNode(formNode);
		return this;
	}

	public boolean isPublish() {
		return publish;
	}

	public boolean handleCookies() {
		return handleCookies;
	}

	public String[] getLanguages() {
		return languages;
	}
}
