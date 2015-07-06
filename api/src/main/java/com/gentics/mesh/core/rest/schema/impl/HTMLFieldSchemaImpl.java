package com.gentics.mesh.core.rest.schema.impl;

import com.gentics.mesh.core.rest.common.FieldTypes;
import com.gentics.mesh.core.rest.schema.HTMLFieldSchema;

public class HTMLFieldSchemaImpl extends AbstractFieldSchema implements HTMLFieldSchema {

	private String defaultHTML;

	@Override
	public String getHTML() {
		return defaultHTML;
	}

	@Override
	public void setHTML(String html) {
		this.defaultHTML = html;
	}

	@Override
	public String getType() {
		return FieldTypes.HTML.toString();
	}
}
