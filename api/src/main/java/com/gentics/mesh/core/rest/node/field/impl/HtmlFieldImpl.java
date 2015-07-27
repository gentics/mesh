package com.gentics.mesh.core.rest.node.field.impl;

import com.gentics.mesh.core.rest.common.FieldTypes;
import com.gentics.mesh.core.rest.node.field.HTMLField;

public class HtmlFieldImpl implements HTMLField {

	private String html;

	//TODO: decide on any special config properties for HTML type, e.g. allowed tags.

	@Override
	public String getHTML() {
		return html;
	}

	@Override
	public HtmlFieldImpl setHTML(String html) {
		this.html = html;
		return this;
	}

	@Override
	public String getType() {
		return FieldTypes.HTML.toString();
	}

}
