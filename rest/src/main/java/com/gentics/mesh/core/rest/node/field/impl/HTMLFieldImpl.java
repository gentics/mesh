package com.gentics.mesh.core.rest.node.field.impl;

import com.gentics.mesh.core.rest.node.field.HTMLField;
import com.gentics.mesh.model.FieldTypes;

public class HTMLFieldImpl implements HTMLField {

	private String html;

	//TODO: decide on any special config properties for HTML type, e.g. allowed tags.

	@Override
	public String getHtml() {
		return html;
	}

	@Override
	public void setHtml(String html) {
		this.html = html;
	}

	@Override
	public String getType() {
		return FieldTypes.HTML.toString();
	}

}
