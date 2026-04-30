package com.gentics.mesh.plugin.forms.context;

import java.util.HashSet;
import java.util.Set;

import com.gentics.mesh.core.rest.node.NodeResponse;
import com.gentics.mesh.plugin.PluginContext;
import com.gentics.mesh.plugin.forms.model.Form;

import io.reactivex.Single;
import io.vertx.core.MultiMap;
import io.vertx.ext.web.FileUpload;

public class FormDataValidateContext {

	private static final String CTX_KEY_FORM = "FDS_Form";
	private static final String CTX_KEY_FORMNODE = "FDS_FormNode";
	private static final String CTX_KEY_FORMDATANODE = "FDS_FormDataNode";

	private final PluginContext context;
	private final Set<FileUpload> fileUploads;

	/**
	 * Get a Single wrapping context
	 * @param pluginContext plugin context
	 * @return single
	 */
	public static Single<FormDataValidateContext> getValidate(PluginContext pluginContext) {
		return Single.just(new FormDataValidateContext(pluginContext));
	}

	protected FormDataValidateContext(PluginContext pluginContext) {
		this.context = pluginContext;
		fileUploads = new HashSet<>(getContext().fileUploads());
		// remove empty uploads
		fileUploads.removeIf(up -> up.size() == 0);
	}

	public Form getForm() {
		return context.get(CTX_KEY_FORM);
	}

	public FormDataValidateContext setForm(Form form) {
		context.put(CTX_KEY_FORM, form);
		return this;
	}

	public NodeResponse getFormDataNode() {
		return getContext().get(CTX_KEY_FORMDATANODE);
	}

	public FormDataValidateContext setFormDataNode(NodeResponse formDataNode) {
		getContext().put(CTX_KEY_FORMDATANODE, formDataNode);
		return this;
	}

	public NodeResponse getFormNode() {
		return getContext().get(CTX_KEY_FORMNODE);
	}

	public FormDataValidateContext setFormNode(NodeResponse formNode) {
		getContext().put(CTX_KEY_FORMNODE, formNode);
		return this;
	}

	public PluginContext getContext() {
		return context;
	}

	public MultiMap getPostedData() {
		return getContext().request().formAttributes();
	}

	public String getReferer() {
		return getContext().request().getHeader("Referer");
	}

	public Set<FileUpload> getFileUploads() {
		return fileUploads;
	}
}
