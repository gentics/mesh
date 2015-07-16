package com.gentics.mesh.core.data.node.field.list.impl;

import com.gentics.mesh.core.data.node.field.basic.HTMLField;
import com.gentics.mesh.core.data.node.field.impl.basic.HTMLFieldImpl;
import com.gentics.mesh.core.data.node.field.list.AbstractBasicFieldList;
import com.gentics.mesh.core.data.node.field.list.HtmlFieldList;

public class HtmlFieldListImpl extends AbstractBasicFieldList<HTMLField> implements HtmlFieldList {

	@Override
	public HTMLField createHTML(String html) {
		HTMLField field = createField();
		field.setHTML(html);
		return field;
	}

	@Override
	protected HTMLField createField(String key) {
		return new HTMLFieldImpl(key, getImpl());
	}
	
	@Override
	public Class<? extends HTMLField> getListType() {
		return HTMLFieldImpl.class;
	}

}
