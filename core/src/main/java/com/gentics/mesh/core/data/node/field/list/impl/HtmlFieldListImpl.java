package com.gentics.mesh.core.data.node.field.list.impl;

import com.gentics.mesh.core.data.node.field.basic.HtmlField;
import com.gentics.mesh.core.data.node.field.impl.basic.HtmlFieldImpl;
import com.gentics.mesh.core.data.node.field.list.AbstractBasicFieldList;
import com.gentics.mesh.core.data.node.field.list.HtmlFieldList;

public class HtmlFieldListImpl extends AbstractBasicFieldList<HtmlField> implements HtmlFieldList {

	@Override
	public HtmlField createHTML(String html) {
		HtmlField field = createField();
		field.setHTML(html);
		return field;
	}

	@Override
	protected HtmlField createField(String key) {
		return new HtmlFieldImpl(key, getImpl());
	}

	@Override
	public HtmlField getHTML(int index) {
		return getField(index);
	}

	@Override
	public Class<? extends HtmlField> getListType() {
		return HtmlFieldImpl.class;
	}

}
