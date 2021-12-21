package com.gentics.mesh.core.data.node.field.impl;

import java.util.Objects;

import com.gentics.mesh.context.BulkActionContext;
import com.gentics.mesh.core.data.HibFieldContainer;
import com.gentics.mesh.core.data.node.field.AbstractBasicField;
import com.gentics.mesh.core.data.node.field.HtmlGraphField;
import com.gentics.mesh.core.rest.node.field.HtmlField;
import com.syncleus.ferma.AbstractVertexFrame;

/**
 * @see HtmlGraphField
 */
public class HtmlGraphFieldImpl extends AbstractBasicField<HtmlField> implements HtmlGraphField {



	public HtmlGraphFieldImpl(String fieldKey, AbstractVertexFrame parentContainer) {
		super(fieldKey, parentContainer);
	}

	@Override
	public void setHtml(String html) {
		setFieldProperty("html", html);
	}

	@Override
	public String getHTML() {
		return getFieldProperty("html");
	}

	@Override
	public void removeField(BulkActionContext bac, HibFieldContainer container) {
		//TODO remove the vertex from the graph if it is no longer be used by other containers 
		setFieldProperty("html", null);
		setFieldKey(null);
	}

	@Override
	public boolean equals(Object obj) {
		if (obj instanceof HtmlGraphField) {
			String htmlA = getHTML();
			String htmlB = ((HtmlGraphField) obj).getHTML();
			return Objects.equals(htmlA, htmlB);
		}
		if (obj instanceof HtmlField) {
			String htmlA = getHTML();
			String htmlB = ((HtmlField) obj).getHTML();
			return Objects.equals(htmlA, htmlB);
		}
		return false;
	}

}
