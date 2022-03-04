package com.gentics.mesh.core.data.node.field.list.impl;

import com.gentics.madl.index.IndexHandler;
import com.gentics.madl.type.TypeHandler;
import com.gentics.mesh.context.BulkActionContext;
import com.gentics.mesh.core.data.generic.MeshVertexImpl;
import com.gentics.mesh.core.data.node.field.HibHtmlField;
import com.gentics.mesh.core.data.node.field.HtmlGraphField;
import com.gentics.mesh.core.data.node.field.impl.HtmlGraphFieldImpl;
import com.gentics.mesh.core.data.node.field.list.AbstractBasicGraphFieldList;
import com.gentics.mesh.core.data.node.field.list.HtmlGraphFieldList;
import com.gentics.mesh.core.rest.node.field.list.impl.HtmlFieldListImpl;

/**
 * @see HtmlGraphFieldList
 */
public class HtmlGraphFieldListImpl extends AbstractBasicGraphFieldList<HibHtmlField, HtmlFieldListImpl, String> implements HtmlGraphFieldList {

	/**
	 * Initialize the vertex type and index.
	 * 
	 * @param type
	 * @param index
	 */
	public static void init(TypeHandler type, IndexHandler index) {
		type.createVertexType(HtmlGraphFieldListImpl.class, MeshVertexImpl.class);
	}

	@Override
	public HibHtmlField createHTML(String html) {
		HibHtmlField field = createField();
		field.setHtml(html);
		return field;
	}

	@Override
	protected HtmlGraphField createField(String key) {
		return new HtmlGraphFieldImpl(key, this);
	}

	@Override
	public HibHtmlField getHTML(int index) {
		return getField(index);
	}

	@Override
	public Class<? extends HibHtmlField> getListType() {
		return HtmlGraphFieldImpl.class;
	}

	@Override
	public void delete(BulkActionContext bac) {
		getElement().remove();
	}
}
