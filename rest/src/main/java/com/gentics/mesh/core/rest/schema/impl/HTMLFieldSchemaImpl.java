package com.gentics.mesh.core.rest.schema.impl;

import com.gentics.mesh.core.rest.common.response.FieldTypes;
import com.gentics.mesh.core.rest.schema.HTMLFieldSchema;

public class HTMLFieldSchemaImpl extends AbstractFieldSchema implements HTMLFieldSchema {

	private String defaultHTML;

	@Override
	public String getHtml() {
		return defaultHTML;
	}

	@Override
	public void setHtml(String html) {
		this.defaultHTML = html;
	}

	@Override
	public String getType() {
		return FieldTypes.HTML.toString();
	}
}
