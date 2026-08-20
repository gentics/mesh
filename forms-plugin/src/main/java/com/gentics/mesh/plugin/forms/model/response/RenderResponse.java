package com.gentics.mesh.plugin.forms.model.response;

import com.gentics.mesh.core.rest.common.RestModel;

public class RenderResponse implements RestModel {
	private String html;

	public String getHtml() {
		return html;
	}

	public RenderResponse setHtml(String html) {
		this.html = html;
		return this;
	}
}
