package com.gentics.mesh.core.rest.schema;

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

}
