package com.gentics.mesh.core.data.node.field.list.impl;

import com.gentics.mesh.core.data.node.field.basic.HTMLField;
import com.gentics.mesh.core.data.node.field.list.AbstractBasicFieldList;
import com.gentics.mesh.core.data.node.field.list.HtmlFieldList;

public class HtmlFieldListImpl extends AbstractBasicFieldList<HTMLField> implements HtmlFieldList  {

	@Override
	public HTMLField createHTML(String key) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	protected HTMLField convertBasicValue(String listItemValue) {
//		return new HTMLFieldImpl(null, this);
		return null;
	}

}
