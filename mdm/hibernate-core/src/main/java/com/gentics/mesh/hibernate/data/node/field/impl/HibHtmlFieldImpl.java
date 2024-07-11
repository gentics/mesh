package com.gentics.mesh.hibernate.data.node.field.impl;

import com.gentics.mesh.core.data.node.field.HtmlField;
import com.gentics.mesh.core.rest.common.FieldTypes;
import com.gentics.mesh.hibernate.data.domain.HibUnmanagedFieldContainer;

/**
 * HTML field of Hibernate content.
 * 
 * @author plyhun
 *
 */
public class HibHtmlFieldImpl extends AbstractBasicHibField<String> implements HtmlField {

	public HibHtmlFieldImpl(String fieldKey, HibUnmanagedFieldContainer<?, ?, ?, ?, ?> parent, String value) {
		super(fieldKey, parent, FieldTypes.HTML, value);
	}

	@Override
	public void setHtml(String html) {
		storeValue(html);
	}

	@Override
	public String getHTML() {
		return valueOrNull();
	}

	@Override
	public boolean equals(Object obj) {
		return htmlEquals(obj);
	}
}
