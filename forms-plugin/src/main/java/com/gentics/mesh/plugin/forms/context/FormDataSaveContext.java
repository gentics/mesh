package com.gentics.mesh.plugin.forms.context;

import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

import com.gentics.mesh.core.rest.node.NodeResponse;
import com.gentics.mesh.plugin.PluginContext;
import com.gentics.mesh.plugin.forms.model.Form;
import com.gentics.mesh.plugin.forms.util.ParameterUtil;

import io.vertx.core.MultiMap;
import io.vertx.ext.web.FileUpload;

/**
 * This context holds everything that the form data update/create handlers need.
 */
public class FormDataSaveContext {
	private static final String CTX_KEY_FORM = "FDS_Form";
	private static final String CTX_KEY_FORMNODE = "FDS_FormNode";
	private static final String CTX_KEY_FORMDATANODE = "FDS_FormDataNode";

	private final PluginContext context;
	private final Set<FileUpload> fileUploads;
	private final boolean handleCookies;

	public FormDataSaveContext(PluginContext pluginContext, boolean handleCookies) {
		this.context = pluginContext;
		this.handleCookies = handleCookies;
		fileUploads = new HashSet<>(context.fileUploads());
		// remove empty uploads
		fileUploads.removeIf(up -> up.size() == 0);
	}

	public String getProjectName() {
		return context.project().getString("name");
	}

	public String getFormUuid() {
		return context.pathParam("formUuid");
	}

	public MultiMap getPostedData() {
		return context.request().formAttributes();
	}

	public String getReferer() {
		return context.request().getHeader("Referer");
	}

	public Set<FileUpload> getFileUploads() {
		return fileUploads;
	}

	public PluginContext getContext() {
		return context;
	}

	public Form getForm() {
		return context.get(CTX_KEY_FORM);
	}

	public FormDataSaveContext setForm(Form form) {
		context.put(CTX_KEY_FORM, form);
		return this;
	}

	public NodeResponse getFormDataNode() {
		return context.get(CTX_KEY_FORMDATANODE);
	}

	public FormDataSaveContext setFormDataNode(NodeResponse formDataNode) {
		context.put(CTX_KEY_FORMDATANODE, formDataNode);
		return this;
	}

	public NodeResponse getFormNode() {
		return context.get(CTX_KEY_FORMNODE);
	}

	public FormDataSaveContext setFormNode(NodeResponse formNode) {
		context.put(CTX_KEY_FORMNODE, formNode);
		return this;
	}

	public boolean isPublish() {
		return Optional.ofNullable(context.queryParams().get("publish"))
			.map(param -> !"false".equals(param))
			.orElse(true);
	}

	public boolean handleCookies() {
		return handleCookies;
	}

	public String getFormDataUuid() {
		return context.pathParam("formDataUuid");
	}

	public String[] getLanguages() {
		return ParameterUtil.getLanguages(context);
	}
}
