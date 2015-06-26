package com.gentics.mesh.core.rest.node.response.field;

import com.gentics.mesh.model.FieldTypes;

public class HTMLField extends AbstractField {

	private String html;

	//TODO: decide on any special config properties for HTML type, e.g. allowed tags.

	public String getHtml() {
		return html;
	}

	public void setHtml(String html) {
		this.html = html;
	}

	@Override
	public String getType() {
		return FieldTypes.HTML.toString();
	}

}
